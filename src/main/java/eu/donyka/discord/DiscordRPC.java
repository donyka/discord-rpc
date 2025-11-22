package eu.donyka.discord;

import com.google.gson.JsonObject;
import eu.donyka.discord.connection.RPCConnection;
import eu.donyka.discord.discord.RichPresence;
import eu.donyka.discord.enums.ErrorCode;
import eu.donyka.discord.exceptions.NoDiscordClientException;
import eu.donyka.discord.exceptions.PipeAccessDenied;
import eu.donyka.discord.exceptions.UnsupportedOsType;
import eu.donyka.discord.models.User;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class DiscordRPC {
    private boolean isDebugMode = false;

    private final boolean disableIoThread;
    private long pid;
    private long nonce;
    private Consumer<User> onReady;
    private Consumer<ErrorInfo> onDisconnected;
    private Consumer<ErrorInfo> onErrored;
    private RPCConnection rpcConnection;

    private ErrorCode lastErrorCode;
    private String lastErrorMessage;
    private ErrorCode lastDisconnectErrorCode;
    private String lastDisconnectErrorMessage;

    private final AtomicBoolean wasJustConnected;
    private final AtomicReference<User> connectedUser;
    private final AtomicBoolean wasJustDisconnected;
    private final AtomicBoolean gotErrorMessage;

    private final Queue<byte[]> sendQueue;
    private final Queue<byte[]> presenceQueue;

    private final AtomicBoolean keepRunning;
    private final Lock waitForIoMutex;
    private final Condition waitForIOActivity;
    private Thread ioThread;

    public DiscordRPC() {
        this(false);
    }

    public DiscordRPC(boolean disableIoThread) {
        this.disableIoThread = disableIoThread;

        this.pid = -1;
        this.nonce = -1;
        this.onReady = null;
        this.onDisconnected = null;
        this.onErrored = null;
        this.rpcConnection = null;

        this.wasJustConnected = new AtomicBoolean(false);
        this.connectedUser = new AtomicReference<>();
        this.wasJustDisconnected = new AtomicBoolean(false);
        this.gotErrorMessage = new AtomicBoolean(false);

        this.sendQueue = new ConcurrentLinkedQueue<>();
        this.presenceQueue = new ConcurrentLinkedQueue<>();

        this.keepRunning = new AtomicBoolean(true);
        this.waitForIoMutex = new ReentrantLock(true);
        this.waitForIOActivity = this.waitForIoMutex.newCondition();
        this.ioThread = null;
    }

    public void init(String applicationId, boolean autoRegister) throws UnsupportedOsType, PipeAccessDenied {
        this.init(applicationId, autoRegister, null);
    }

    public void init(String applicationId, boolean autoRegister, String optionalSteamId) throws UnsupportedOsType, PipeAccessDenied {
        if (this.rpcConnection != null) {
            return;
        }

        this.pid = this.getProcessId();

        try {
            this.rpcConnection = RPCConnection.create(applicationId, this);
        } catch (UnsupportedOsType e) {
            throw e;
        }

        if (autoRegister) {
            if (optionalSteamId != null && !optionalSteamId.isEmpty()) {
                this.rpcConnection.getBaseConnection().registerSteamGame(applicationId, optionalSteamId);
            } else {
                this.rpcConnection.getBaseConnection().register(applicationId, null);
            }
        }

        this.rpcConnection.setConnectedCallback((user) -> {
            this.wasJustConnected.set(true);
            this.connectedUser.set(user);

            if (this.onReady != null) {
                this.onReady.accept(user);
            }
        });

        this.rpcConnection.setDisconnectedCallback((RPCConnection.ErrorInfo errorInfo) -> {
            this.lastDisconnectErrorCode = errorInfo.getErrorCode();
            this.lastDisconnectErrorMessage = errorInfo.getMessage();
            this.wasJustDisconnected.set(true);
        });

        if (!this.disableIoThread) {
            this.keepRunning.set(true);
            this.ioThread = new Thread(() -> {
                try {
                    this.discordRpcIo();
                } catch (NoDiscordClientException | PipeAccessDenied ignored) {
                }
            });
            this.ioThread.setDaemon(true);
            this.ioThread.start();
        }
    }

    public void shutdown() {
        if (this.rpcConnection == null) {
            return;
        }

        this.rpcConnection.setDisconnectedCallback(null);
        this.rpcConnection.setConnectedCallback(null);
        this.onReady = null;
        this.onDisconnected = null;
        this.onErrored = null;

        if (!this.disableIoThread) {
            this.keepRunning.set(false);
            this.signalIoActivity();

            try {
                if (this.ioThread != null) {
                    this.ioThread.join();
                }
            } catch (Exception ignored) {
            }
        }

        RPCConnection.destroy(this.rpcConnection);
        this.rpcConnection = null;
    }

    public void updatePresence(RichPresence richPresence) {
        if (richPresence == null) {
            richPresence = RichPresence.builder().build();
        }

        JsonObject data = richPresence.toJson(this.pid, this.nonce++);
        this.presenceQueue.offer(data.toString().getBytes(StandardCharsets.UTF_8));
        this.signalIoActivity();
    }

    public void runCallbacks() {
        if (this.rpcConnection == null) {
            return;
        }

        boolean wasDisconnected = this.wasJustDisconnected.getAndSet(false);
        boolean isConnected = this.rpcConnection.isOpen();

        if (isConnected && wasDisconnected && this.onDisconnected != null) {
            this.onDisconnected.accept(new ErrorInfo(this.lastDisconnectErrorCode, this.lastDisconnectErrorMessage));
        }

        if (this.wasJustConnected.getAndSet(false) && this.onReady != null) {
            this.onReady.accept(connectedUser.get());
        }

        if (this.gotErrorMessage.getAndSet(false) && this.onErrored != null) {
            this.onErrored.accept(new ErrorInfo(this.lastErrorCode, this.lastErrorMessage));
        }

        if (!isConnected && wasDisconnected && this.onDisconnected != null) {
            this.onDisconnected.accept(new ErrorInfo(this.lastDisconnectErrorCode, this.lastDisconnectErrorMessage));
        }
    }

    private void discordRpcIo() throws NoDiscordClientException, PipeAccessDenied {
        while (this.keepRunning.get()) {
            try {
                this.updateConnection();
            } catch (NoDiscordClientException | PipeAccessDenied ignored) {
            }

            runCallbacks();
            this.waitForIoMutex.lock();

            try {
                this.waitForIOActivity.await(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                this.waitForIoMutex.unlock();
            }
        }
    }

    private void signalIoActivity() {
        this.waitForIoMutex.lock();

        try {
            this.waitForIOActivity.signalAll();
        } catch (Exception ignored) {
        } finally {
            this.waitForIoMutex.unlock();
        }
    }

    public void updateConnection() throws NoDiscordClientException, PipeAccessDenied {
        if (this.rpcConnection == null) {
            return;
        }

        if (!this.rpcConnection.isOpen()) {
            this.rpcConnection.open();
        } else {
            JsonObject message = new JsonObject();

            while (true) {
                if (!this.rpcConnection.read(message)) {
                    break;
                }

                String evtName = message.has("evt") && !message.get("evt").isJsonNull() ? message.get("evt").getAsString() : null;
                String nonce = message.has("nonce") && !message.get("nonce").isJsonNull() ? message.get("nonce").getAsString() : null;

                if (nonce != null) {
                    if (evtName != null && evtName.equals("ERROR")) {
                        JsonObject data = message.get("data").getAsJsonObject();
                        int error = data.get("code").getAsInt();
                        this.lastErrorCode = error >= ErrorCode.values().length ? ErrorCode.UNKNOWN : ErrorCode.values()[error];
                        this.lastErrorMessage = data.has("message") ? data.get("message").getAsString() : "";
                        this.gotErrorMessage.set(true);
                    }
                }
            }

            if (!this.presenceQueue.isEmpty()) {
                byte[] bytes;

                while ((bytes = this.presenceQueue.peek()) != null) {
                    if (!this.rpcConnection.write(bytes)) {
                        break;
                    } else {
                        this.presenceQueue.poll();
                    }
                }
            }

            if (!this.sendQueue.isEmpty()) {
                byte[] bytes;

                while ((bytes = this.sendQueue.poll()) != null) {
                    this.rpcConnection.write(bytes);
                }
            }
        }
    }

    private long getProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf('@');

        if (index < 1) {
            return -1;
        }

        try {
            return Long.parseLong(jvmName.substring(0, index));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void setOnReady(Consumer<User> onReady) {
        this.onReady = onReady;
    }

    public void setOnDisconnected(Consumer<ErrorInfo> onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    public void setOnErrored(Consumer<ErrorInfo> onErrored) {
        this.onErrored = onErrored;
    }

    public void setDebugMode(boolean debugMode) {
        this.isDebugMode = debugMode;
    }

    public boolean isDebugMode() {
        return isDebugMode;
    }

    public static class ErrorInfo {
        private final ErrorCode errorCode;
        private final String message;

        public ErrorInfo(ErrorCode errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ErrorCode: " + errorCode + " - " + message;
        }
    }
}

