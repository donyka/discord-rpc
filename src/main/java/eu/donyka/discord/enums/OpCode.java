package eu.donyka.discord.enums;

import lombok.Getter;

@Getter
public enum OpCode {
    HANDSHAKE(0),
    FRAME(1),
    CLOSE(2),
    PING(3),
    PONG(4);

    private final int id;

    OpCode(int id) {
        this.id = id;
    }

}

