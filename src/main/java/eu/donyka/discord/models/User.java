package eu.donyka.discord.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class User {
    private String userId;
    private String username;
    private String discriminator;
    private String avatar;

    public User() {
    }

    public User(String userId, String username, String discriminator, String avatar) {
        this.userId = userId;
        this.username = username;
        this.discriminator = discriminator;
        this.avatar = avatar;
    }
}

