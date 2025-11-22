

import eu.donyka.discord.RPCHandler;
import eu.donyka.discord.discord.RichPresence;
import eu.donyka.discord.discord.RichPresenceBuilder;
import eu.donyka.discord.exceptions.UnsupportedOsType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RPCTest {

    public static void main(String[] args) throws UnsupportedOsType {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

        RPCHandler.setOnReady(user -> {
            RichPresence presence = RichPresence.builder()
                    .details("Version: 1.0.0")
                    .state("State")
                    .largeImageKey("https://r2.e-z.host/3b5c6e0e-0b0d-4347-b5f0-73330275f31d/dplnyr44.gif")
                    .largeImageText(user.getUsername())
                    .button(RichPresenceBuilder.RPCButton.of("Button 1", "https://example.com"))
                    .button(RichPresenceBuilder.RPCButton.of("Button 2", "https://example.com"))
                    .build();

            RPCHandler.updatePresence(presence);
        });

        RPCHandler.setOnDisconnected(error -> {
            System.out.println("RPC Disconnected: " + error);
        });

        RPCHandler.setOnErrored(error -> {
            System.out.println("RPC Errored: " + error);
        });

        RPCHandler.startup("1386072578061373661", false);

        scheduledExecutorService.scheduleAtFixedRate(() -> {}, 0, 500, TimeUnit.MILLISECONDS);
    }

}
