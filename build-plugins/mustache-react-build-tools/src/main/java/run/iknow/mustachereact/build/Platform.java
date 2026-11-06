package run.iknow.mustachereact.build;

import java.util.Locale;

/**
 * The operating system and CPU, named as in Node.js distribution files ({@code node-v24.1.0-linux-x64.tar.gz}).
 */
public final class Platform {

    private final String os;
    private final String arch;

    Platform(String os, String arch) {
        this.os = os;
        this.arch = arch;
    }

    /**
     * @return the platform this JVM runs on
     * @throws IllegalStateException on a platform Node.js does not ship for
     */
    public static Platform current() {
        return of(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    static Platform of(String osName, String osArch) {
        String name = osName.toLowerCase(Locale.ROOT);
        String os;
        if (name.contains("win")) {
            os = "win";
        } else if (name.contains("mac") || name.contains("darwin")) {
            os = "darwin";
        } else if (name.contains("linux")) {
            os = "linux";
        } else {
            throw new IllegalStateException("No Node.js download for the operating system '" + osName
                    + "'. Install Node.js yourself and turn the download off.");
        }
        String archName = osArch.toLowerCase(Locale.ROOT);
        String arch;
        if (archName.equals("amd64") || archName.equals("x86_64")) {
            arch = "x64";
        } else if (archName.equals("aarch64") || archName.equals("arm64")) {
            arch = "arm64";
        } else {
            throw new IllegalStateException("No Node.js download for the CPU '" + osArch
                    + "'. Install Node.js yourself and turn the download off.");
        }
        return new Platform(os, arch);
    }

    /** @return {@code true} on Windows */
    public boolean isWindows() {
        return "win".equals(os);
    }

    /**
     * @param version Node.js version without {@code v}, e.g. {@code 24.22.2}
     * @return the directory name inside the archive, e.g. {@code node-v24.22.2-linux-x64}
     */
    public String distributionName(String version) {
        return "node-v" + version + "-" + os + "-" + arch;
    }

    /**
     * @param version Node.js version without {@code v}
     * @return the archive file name, e.g. {@code node-v24.22.2-linux-x64.tar.gz}
     */
    public String archiveName(String version) {
        return distributionName(version) + (isWindows() ? ".zip" : ".tar.gz");
    }

    @Override
    public String toString() {
        return os + "-" + arch;
    }
}
