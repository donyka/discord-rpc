package eu.donyka.discord.connection.unix;

import java.io.IOException;

public interface IUnixBackend {
    void openPipe(String path) throws IOException;

    void closePipe() throws IOException;

    void write(byte[] bytes) throws IOException;

    int getAvailable() throws IOException;

    int read(byte[] bytes) throws IOException;

    boolean isConnected();
}

