package eu.donyka.discord.connection.unix;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;

public class JUnixBackend implements IUnixBackend {
    private AFUNIXSocket socket;

    @Override
    public void openPipe(String path) throws IOException {
        AFUNIXSocket socket = AFUNIXSocket.newInstance();
        try {
            socket.connect(AFUNIXSocketAddress.of(new File(path)));
            this.socket = socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    @Override
    public void closePipe() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (socket == null || !socket.isConnected()) {
            return;
        }
        socket.getOutputStream().write(bytes);
    }

    @Override
    public int getAvailable() throws IOException {
        if (socket == null || !socket.isConnected()) {
            return -1;
        }
        return socket.getInputStream().available();
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        if (socket == null || !socket.isConnected()) {
            return -1;
        }
        return socket.getInputStream().read(bytes);
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }
}

