package eu.donyka.discord.models;

import eu.donyka.discord.enums.OpCode;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Getter
public class MessageFrame {
    byte[] headerBuffer;
    byte[] messageBuffer;

    @Setter
    private OpCode opCode;
    private int length;
    private String message;

    public MessageFrame() {
        this.headerBuffer = new byte[8];
        this.messageBuffer = new byte[65535 - this.headerBuffer.length];
    }

    public MessageFrame(OpCode code, String message) {
        this();
        this.opCode = code;
        this.message = message;
    }

    public boolean parseHeader() {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(this.headerBuffer)) {
            this.opCode = OpCode.values()[this.readInt(inputStream)];
            this.length = this.readInt(inputStream);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private int readInt(InputStream stream) throws IOException {
        int ch1 = stream.read();
        int ch2 = stream.read();
        int ch3 = stream.read();
        int ch4 = stream.read();

        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();

        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
    }

    public boolean parseMessage() {
        this.message = new String(Arrays.copyOfRange(this.messageBuffer, 0, this.length), StandardCharsets.UTF_8);
        return true;
    }

    public ByteBuffer write() {
        byte[] d = message.getBytes(StandardCharsets.UTF_8);

        ByteBuffer writeStream = ByteBuffer.allocate(d.length + 8);
        writeStream.putInt(Integer.reverseBytes(opCode.ordinal()));
        writeStream.putInt(Integer.reverseBytes(d.length));
        writeStream.put(d);
        writeStream.rewind();

        return writeStream;
    }
}

