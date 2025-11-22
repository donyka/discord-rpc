package eu.donyka.discord.connection;

import eu.donyka.discord.DiscordRPC;
import eu.donyka.discord.exceptions.NoDiscordClientException;
import eu.donyka.discord.exceptions.PipeAccessDenied;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

class WindowsConnection extends BaseConnection {
    private RandomAccessFile pipe;
    private boolean opened;

    WindowsConnection(DiscordRPC rpc) {
        super(rpc);
        this.pipe = null;
        this.opened = false;
    }

    @Override
    boolean isOpen() {
        return this.opened;
    }

    @Override
    boolean open() throws NoDiscordClientException, PipeAccessDenied {
        String pipeName = "\\\\.\\pipe\\discord-ipc-%d";

        if (this.isOpen()) {
            throw new IllegalStateException("Connection is already opened");
        }

        for (int i = 0; i < 10; i++) {
            String pipePath = String.format(pipeName, i);

            try {
                if (!new File(pipePath).exists()) {
                    continue;
                }

                this.pipe = new RandomAccessFile(pipePath, "rw");
                this.opened = true;
                return true;
            } catch (FileNotFoundException e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("access is denied")) {
                    throw new PipeAccessDenied("Cannot access pipe " + String.format(pipeName, i) + " due to permission errors. Ensure discord is NOT running in administrator mode!");
                }
            } catch (SecurityException sec) {
                throw new PipeAccessDenied("Failed to open RPC Connection, with error Access Denied. Is Discord running in Administrator mode?");
            }
        }

        throw new NoDiscordClientException();
    }

    @Override
    void close() {
        if (!this.isOpen()) {
            return;
        }

        try {
            this.pipe.close();
        } catch (Exception ignored) {
        }

        this.opened = false;
    }

    @Override
    boolean write(byte[] bytes) {
        if (!this.isOpen()) {
            return false;
        }

        try {
            this.pipe.write(bytes);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    boolean read(byte[] bytes, int length) {
        if (bytes == null || bytes.length == 0) {
            return bytes != null;
        }

        if (!this.isOpen()) {
            return false;
        }

        try {
            long available = this.pipe.length() - this.pipe.getFilePointer();
            if (available < length) {
                return false;
            }

            int read = this.pipe.read(bytes, 0, length);

            if (read != length) {
                throw new IOException("Read less data than supplied. Expected: " + length + ". Got: " + read);
            }

            return true;
        } catch (IOException e) {
            this.close();
            return false;
        }
    }

    @Override
    public void register(String applicationId, String command) {
        String javaLibraryPath = System.getProperty("java.home");
        File javaExeFile = new File(javaLibraryPath.split(";")[0] + "/bin/java.exe");
        File javawExeFile = new File(javaLibraryPath.split(";")[0] + "/bin/javaw.exe");
        String javaExePath = javaExeFile.exists() ? javaExeFile.getAbsolutePath() : javawExeFile.exists() ? javawExeFile.getAbsolutePath() : null;

        if (javaExePath == null)
            throw new RuntimeException("Unable to find java path");

        String openCommand;

        if (command != null)
            openCommand = command;
        else
            openCommand = javaExePath;

        String protocolName = "discord-" + applicationId;
        String protocolDescription = "URL:Run game " + applicationId + " protocol";
        String keyName = "Software\\Classes\\" + protocolName;
        String iconKeyName = keyName + "\\DefaultIcon";
        String commandKeyName = keyName + "\\DefaultIcon";

        try {
            WinRegistry.createKey(keyName);
            WinRegistry.writeStringValue(keyName, "", protocolDescription);
            WinRegistry.writeStringValue(keyName, "URL Protocol", "\0");

            WinRegistry.createKey(iconKeyName);
            WinRegistry.writeStringValue(iconKeyName, "", javaExePath);

            WinRegistry.createKey(commandKeyName);
            WinRegistry.writeStringValue(commandKeyName, "", openCommand);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to modify Discord registry keys", ex);
        }
    }

    @Override
    public void registerSteamGame(String applicationId, String steamId) {
        try {
            String steamPath = WinRegistry.readString();
            if (steamPath == null)
                throw new RuntimeException("Steam exe path not found");

            steamPath = steamPath.replaceAll("/", "\\");
            String command = "\"" + steamPath + "\" steam://rungameid/" + steamId;
            this.register(applicationId, command);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to register Steam game", ex);
        }
    }
}

