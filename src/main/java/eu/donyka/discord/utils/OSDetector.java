package eu.donyka.discord.utils;

public class OSDetector {
    public static final OSDetector INSTANCE = new OSDetector();

    public OSType detectOs() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return OSType.WINDOWS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return OSType.LINUX;
        } else if (osName.contains("mac")) {
            return OSType.MACOS;
        }
        
        return OSType.UNKNOWN;
    }

    public enum OSType {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN;

        public boolean isWindows() {
            return this == WINDOWS;
        }

        public boolean isLinux() {
            return this == LINUX;
        }

        public boolean isMac() {
            return this == MACOS;
        }
    }
}

