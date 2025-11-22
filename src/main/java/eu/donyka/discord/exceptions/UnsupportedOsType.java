package eu.donyka.discord.exceptions;

public class UnsupportedOsType extends Exception {
    public UnsupportedOsType(String osType) {
        super("Unsupported OS type: " + osType);
    }
}

