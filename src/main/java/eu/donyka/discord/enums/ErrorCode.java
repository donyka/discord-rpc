package eu.donyka.discord.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0),
    PIPE_CLOSED(1),
    READ_CORRUPT(2),
    UNKNOWN(-1),
    USER_LOGOUT(1000);

    private final int id;

    ErrorCode(int id) {
        this.id = id;
    }

}

