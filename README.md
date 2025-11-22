# discord-rpc

[![Release](https://jitpack.io/v/donyka/discord-rpc.svg)](https://jitpack.io/#donyka/discord-rpc)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A lightweight, cross-platform Java library for Discord Rich Presence (RPC) using IPC sockets (no native JNI/DLL binaries required).

## Features

- **Cross-platform**: Supports Windows (Named Pipes), Linux, and macOS (Unix Domain Sockets).
- **Lightweight**: Zero native DLL/SO libraries. Uses `junixsocket` for Unix sockets and `gson`.
- **Easy-to-use**: Simple fluent builder for presence payloads.
- **Event Callbacks**: Callbacks for `onReady`, `onDisconnected`, and `onErrored`.

## Installation

First, add the Maven repository:

```groovy
maven {
    name "JitPack"
    url "https://jitpack.io"
}
```

Next, add the required dependency:

```groovy
implementation "com.github.donyka:discord-rpc:1.0.0"
```

<details>
<summary>Maven (pom.xml)</summary>

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.donyka</groupId>
        <artifactId>discord-rpc</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

</details>

## Usage

```java
import eu.donyka.discord.RPCHandler;
import eu.donyka.discord.discord.RichPresence;
import eu.donyka.discord.discord.RichPresenceBuilder;

public class Main {
    public static void main(String[] args) {
        // Register callbacks
        RPCHandler.setOnReady(user -> {
            System.out.println("Connected as: " + user.getUsername());

            RichPresence presence = RichPresence.builder()
                    .details("Playing")
                    .state("In Menu")
                    .largeImageKey("large_image")
                    .largeImageText("discord-rpc")
                    .button(RichPresenceBuilder.RPCButton.of("GitHub", "https://github.com/donyka/discord-rpc"))
                    .build();

            RPCHandler.updatePresence(presence);
        });

        RPCHandler.setOnDisconnected(error -> {
            System.out.println("Disconnected: " + error);
        });

        RPCHandler.setOnErrored(error -> {
            System.out.println("Error: " + error);
        });

        // Initialize RPC with your Discord Client ID
        RPCHandler.startup("YOUR_CLIENT_ID", false);
    }
}
```

## License

This project is licensed under the [MIT License](LICENSE).
