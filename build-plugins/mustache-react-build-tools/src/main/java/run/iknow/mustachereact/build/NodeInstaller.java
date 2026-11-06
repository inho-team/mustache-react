package run.iknow.mustachereact.build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Downloads a Node.js distribution into a project-local directory ({@code .mustache-react/node} by default) and
 * returns its {@code node} executable. The archive is checked against the release's {@code SHASUMS256.txt}.
 * An installed version is reused.
 */
public final class NodeInstaller {

    /** Default Node.js version: an LTS release. */
    public static final String DEFAULT_VERSION = "24.22.2";

    /** Official distribution site. Mirrors with the same layout work too. */
    public static final String DEFAULT_DISTRIBUTION_URL = "https://nodejs.org/dist";

    private final Path installRoot;
    private final String distributionUrl;
    private final Platform platform;
    private final BuildLog log;

    /**
     * @param installRoot directory holding the installed versions, e.g. {@code <project>/.mustache-react/node}
     * @param distributionUrl base URL of the distribution, e.g. {@code https://nodejs.org/dist}
     * @param log build log
     */
    public NodeInstaller(Path installRoot, String distributionUrl, BuildLog log) {
        this(installRoot, distributionUrl, Platform.current(), log);
    }

    NodeInstaller(Path installRoot, String distributionUrl, Platform platform, BuildLog log) {
        this.installRoot = installRoot;
        this.distributionUrl = distributionUrl.endsWith("/")
                ? distributionUrl.substring(0, distributionUrl.length() - 1) : distributionUrl;
        this.platform = platform;
        this.log = log;
    }

    /**
     * Installs a version unless it is already installed.
     *
     * @param version Node.js version, with or without the leading {@code v}
     * @return the Node.js installation
     * @throws IOException if the download, the checksum or the extraction fails
     */
    public NodeRuntime install(String version) throws IOException {
        String bare = version.startsWith("v") ? version.substring(1) : version;
        Path home = installRoot.resolve(platform.distributionName(bare));
        NodeRuntime runtime = NodeRuntime.installed(home, platform);
        if (Files.isRegularFile(runtime.node())) {
            return runtime;
        }

        String archiveName = platform.archiveName(bare);
        String base = distributionUrl + "/v" + bare + "/";
        log.info("mustache-react: downloading Node.js " + bare + " (" + platform + ") from " + base);
        Files.createDirectories(installRoot);
        Path work = Files.createTempDirectory(installRoot, ".download-");
        try {
            String expected = checksum(base + "SHASUMS256.txt", archiveName);
            Path archive = work.resolve(archiveName);
            String actual = download(base + archiveName, archive);
            if (!expected.equalsIgnoreCase(actual)) {
                throw new IOException("Checksum mismatch for " + archiveName + ": expected " + expected + ", got " + actual);
            }
            Path extracted = work.resolve("extracted");
            Archives.extract(archive, extracted);
            Path unpacked = extracted.resolve(platform.distributionName(bare));
            if (!Files.isDirectory(unpacked)) {
                throw new IOException(archiveName + " does not contain " + platform.distributionName(bare));
            }
            try {
                Files.move(unpacked, home, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(unpacked, home);
            } catch (FileAlreadyExistsException ex) {
                // installed concurrently by another build
            }
        } finally {
            deleteRecursively(work);
        }
        if (!Files.isRegularFile(runtime.node())) {
            throw new IOException("Node.js was extracted but " + runtime.node() + " is missing");
        }
        return runtime;
    }

    private static String checksum(String url, String archiveName) throws IOException {
        HttpURLConnection connection = open(url);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2 && parts[1].equals(archiveName)) {
                    return parts[0];
                }
            }
        } finally {
            connection.disconnect();
        }
        throw new IOException(url + " lists no checksum for " + archiveName);
    }

    private static String download(String url, Path target) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
        HttpURLConnection connection = open(url);
        try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
                out.write(buffer, 0, n);
            }
        } finally {
            connection.disconnect();
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(120_000);
        connection.setInstanceFollowRedirects(true);
        int status = connection.getResponseCode();
        if (status != 200) {
            connection.disconnect();
            throw new IOException("GET " + url + " returned HTTP " + status);
        }
        return connection;
    }

    static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }
}
