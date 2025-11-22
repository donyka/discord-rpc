package eu.donyka.discord.connection;

import eu.donyka.discord.DiscordRPC;
import eu.donyka.discord.connection.unix.IUnixBackend;
import eu.donyka.discord.connection.unix.JUnixBackend;
import eu.donyka.discord.exceptions.NoDiscordClientException;
import eu.donyka.discord.exceptions.PipeAccessDenied;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

class UnixConnection extends BaseConnection {
    private IUnixBackend unixBackend;

    UnixConnection(DiscordRPC rpc) {
        super(rpc);
        this.unixBackend = new JUnixBackend();
    }

    @Override
    boolean isOpen() {
        return this.unixBackend != null && this.unixBackend.isConnected();
    }

    @Override
    boolean open() throws NoDiscordClientException, PipeAccessDenied {
        String pipeName = this.getTempPath() + "/discord-ipc-%s";

        if (this.isOpen()) {
            throw new IllegalStateException("Connection is already opened");
        }

        if (this.tryOpenConnection(pipeName)) {
            return true;
        }

        String nextPath = getAdditionalPaths();

        if (nextPath != null && !nextPath.isEmpty() && this.tryOpenConnection(nextPath + "/discord-ipc-%s")) {
            return true;
        }

        throw new NoDiscordClientException();
    }

    private boolean tryOpenConnection(String pipeName) {
        for (int i = 0; i < 10; i++) {
            try {
                File test = new File(String.format(pipeName, i));
                if (!test.exists()) {
                    continue;
                }

                this.unixBackend.openPipe(String.format(pipeName, i));
                return true;
            } catch (Exception e) {
            }
        }

        return false;
    }

    @Override
    void close() {
        if (!this.isOpen()) {
            return;
        }

        try {
            this.unixBackend.closePipe();
        } catch (IOException e) {
        }
    }

    @Override
    boolean write(byte[] bytes) {
        if (!this.isOpen()) {
            return false;
        }

        try {
            this.unixBackend.write(bytes);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    boolean read(byte[] bytes, int length) {
        if (bytes == null || bytes.length == 0) {
            return bytes != null;
        }

        if (!isOpen()) {
            return false;
        }

        try {
            int available = this.unixBackend.getAvailable();

            if (available < length) {
                return false;
            }

            byte[] buf = new byte[length];
            int read = this.unixBackend.read(buf);
            if (read != length) {
                throw new IOException("Read less data than supplied. Expected: " + length + ". Got: " + read);
            }

            ByteBuffer buffer = ByteBuffer.wrap(buf);
            buffer.rewind();
            buffer.get(bytes, 0, length);
            return true;
        } catch (Exception e) {
            this.close();
            return false;
        }
    }

    @Override
    public void register(String applicationId, String command) {
        String home = System.getenv("HOME");

        if (home == null) {
            throw new RuntimeException("Unable to find user HOME directory");
        }

        if (command == null) {
            try {
                command = Files.readSymbolicLink(Paths.get("/proc/self/exe")).toString();
            } catch (Exception ex) {
                throw new RuntimeException("Unable to get current exe path from /proc/self/exe", ex);
            }
        }

        String desktopFile =
                "[Desktop Entry]\n" +
                        "Name=Game " + applicationId + "\n" +
                        "Exec=" + command + " %%u\n" +
                        "Type=Application\n" +
                        "NoDisplay=true\n" +
                        "Categories=Discord;Games;\n" +
                        "MimeType=x-scheme-handler/discord-" + applicationId + ";\n";

        String desktopFileName = "/discord-" + applicationId + ".desktop";
        String desktopFilePath = home + "/.local";

        if (!this.mkdir(desktopFilePath)) {
            throw new RuntimeException("Failed to create directory '" + desktopFilePath + "'");
        }

        desktopFilePath += "/share";

        if (!this.mkdir(desktopFilePath)) {
            throw new RuntimeException("Failed to create directory '" + desktopFilePath + "'");
        }

        desktopFilePath += "/applications";

        if (!this.mkdir(desktopFilePath)) {
            throw new RuntimeException("Failed to create directory '" + desktopFilePath + "'");
        }

        desktopFilePath += desktopFileName;

        try (FileWriter fileWriter = new FileWriter(desktopFilePath)) {
            fileWriter.write(desktopFile);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write desktop info into '" + desktopFilePath + "'");
        }

        String xdgMimeCommand = "xdg-mime default discord-" + applicationId + ".desktop x-scheme-handler/discord-" + applicationId;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(xdgMimeCommand.split(" "));
            processBuilder.environment();
            int result = processBuilder.start().waitFor();
            if (result < 0) {
                throw new Exception("xdg-mime returned " + result);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to register mime handler", ex);
        }
    }

    @Override
    public void registerSteamGame(String applicationId, String steamId) {
        this.register(applicationId, "xdg-open steam://rungameid/" + steamId);
    }

    private String getTempPath() {
        String temp = System.getenv("XDG_RUNTIME_DIR");
        temp = temp != null ? temp : System.getenv("TMPDIR");
        temp = temp != null ? temp : System.getenv("TMP");
        temp = temp != null ? temp : System.getenv("TEMP");
        temp = temp != null ? temp : "/tmp";
        return temp;
    }

    private String getAdditionalPaths() {
        String[] unixFolderPaths = {"/snap.discord", "/app/com.discordapp.Discord"};
        String path = getTempPath();

        for (String s : unixFolderPaths) {
            File f = new File(path, s);
            if (f.exists() && f.isDirectory() && f.list() != null && f.list().length > 0) {
                return f.getAbsolutePath();
            }
        }

        return null;
    }

    boolean mkdir(String path) {
        File file = new File(path);
        return file.exists() && file.isDirectory() || file.mkdir();
    }
}

