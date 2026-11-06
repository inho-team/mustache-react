package run.iknow.mustachereact.build;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts the Node.js distribution archives: {@code .tar.gz} (ustar, with pax and GNU long names) and {@code .zip}.
 * Entries that would land outside the target directory are rejected.
 */
final class Archives {

    private static final int BLOCK = 512;

    private Archives() {
    }

    static void extract(Path archive, Path target) throws IOException {
        Files.createDirectories(target);
        if (archive.getFileName().toString().endsWith(".zip")) {
            extractZip(archive, target);
        } else {
            extractTarGz(archive, target);
        }
    }

    private static void extractZip(Path archive, Path target) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path path = resolve(target, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else {
                    Files.createDirectories(path.getParent());
                    Files.copy(zip, path, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void extractTarGz(Path archive, Path target) throws IOException {
        try (InputStream in = new BufferedInputStream(new GZIPInputStream(Files.newInputStream(archive)))) {
            byte[] header = new byte[BLOCK];
            String longName = null;
            String longLink = null;
            while (true) {
                if (!readFully(in, header)) {
                    return;
                }
                if (isZero(header)) {
                    return;
                }
                String name = string(header, 0, 100);
                String prefix = string(header, 345, 155);
                if (!prefix.isEmpty()) {
                    name = prefix + "/" + name;
                }
                int mode = (int) octal(header, 100, 8);
                long size = octal(header, 124, 12);
                char type = (char) header[156];
                String link = string(header, 157, 100);

                if (type == 'x' || type == 'g' || type == 'L' || type == 'K') {
                    byte[] data = readData(in, size);
                    if (type == 'L') {
                        longName = string(data, 0, data.length);
                    } else if (type == 'K') {
                        longLink = string(data, 0, data.length);
                    } else if (type == 'x') {
                        String path = paxValue(data, "path");
                        String linkpath = paxValue(data, "linkpath");
                        longName = path != null ? path : longName;
                        longLink = linkpath != null ? linkpath : longLink;
                    }
                    continue;
                }
                if (longName != null) {
                    name = longName;
                    longName = null;
                }
                if (longLink != null) {
                    link = longLink;
                    longLink = null;
                }

                Path path = resolve(target, name);
                switch (type) {
                    case '5':
                        Files.createDirectories(path);
                        skip(in, size);
                        break;
                    case '2':
                        Files.createDirectories(path.getParent());
                        Files.deleteIfExists(path);
                        if (!path.getParent().resolve(link).normalize().startsWith(target.normalize())) {
                            throw new IOException("Archive link outside the target directory: " + name + " -> " + link);
                        }
                        Files.createSymbolicLink(path, Paths.get(link));
                        skip(in, size);
                        break;
                    case '0':
                    case '\0':
                        Files.createDirectories(path.getParent());
                        Files.copy(new BoundedInputStream(in, size), path, StandardCopyOption.REPLACE_EXISTING);
                        skipPadding(in, size);
                        setPermissions(path, mode);
                        break;
                    default:
                        skip(in, size); // hard links, devices: not used by Node.js archives
                }
            }
        }
    }

    private static Path resolve(Path target, String name) throws IOException {
        Path path = target.resolve(name).normalize();
        if (!path.startsWith(target.normalize())) {
            throw new IOException("Archive entry outside the target directory: " + name);
        }
        return path;
    }

    private static void setPermissions(Path path, int mode) {
        if ((mode & 0111) == 0) {
            return;
        }
        try {
            Set<PosixFilePermission> permissions = EnumSet.copyOf(Files.getPosixFilePermissions(path));
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            if ((mode & 0010) != 0) {
                permissions.add(PosixFilePermission.GROUP_EXECUTE);
            }
            if ((mode & 0001) != 0) {
                permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            }
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException | IOException ex) {
            path.toFile().setExecutable(true);
        }
    }

    private static String paxValue(byte[] data, String key) {
        String text = new String(data, StandardCharsets.UTF_8);
        for (String line : text.split("\n")) {
            int space = line.indexOf(' ');
            int eq = line.indexOf('=');
            if (space > 0 && eq > space && line.substring(space + 1, eq).equals(key)) {
                return line.substring(eq + 1);
            }
        }
        return null;
    }

    private static byte[] readData(InputStream in, long size) throws IOException {
        byte[] data = new byte[(int) size];
        if (!readFully(in, data)) {
            throw new EOFException("Truncated tar archive");
        }
        skipPadding(in, size);
        return data;
    }

    private static void skip(InputStream in, long size) throws IOException {
        long remaining = size;
        byte[] buffer = new byte[8192];
        while (remaining > 0) {
            int n = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (n < 0) {
                throw new EOFException("Truncated tar archive");
            }
            remaining -= n;
        }
        skipPadding(in, size);
    }

    private static void skipPadding(InputStream in, long size) throws IOException {
        long padding = (BLOCK - size % BLOCK) % BLOCK;
        byte[] buffer = new byte[(int) padding];
        if (padding > 0 && !readFully(in, buffer)) {
            throw new EOFException("Truncated tar archive");
        }
    }

    private static boolean readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int n = in.read(buffer, offset, buffer.length - offset);
            if (n < 0) {
                if (offset == 0) {
                    return false;
                }
                throw new EOFException("Truncated tar archive");
            }
            offset += n;
        }
        return true;
    }

    private static boolean isZero(byte[] block) {
        for (byte b : block) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private static String string(byte[] bytes, int offset, int length) {
        int end = offset;
        while (end < offset + length && bytes[end] != 0) {
            end++;
        }
        return new String(bytes, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private static long octal(byte[] bytes, int offset, int length) {
        String text = string(bytes, offset, length);
        return text.isEmpty() ? 0 : Long.parseLong(text, 8);
    }

    /** Reads at most {@code limit} bytes from the underlying stream, without closing it. */
    private static final class BoundedInputStream extends InputStream {
        private final InputStream in;
        private long remaining;

        BoundedInputStream(InputStream in, long limit) {
            this.in = in;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int b = in.read();
            if (b >= 0) {
                remaining--;
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int n = in.read(buffer, offset, (int) Math.min(length, remaining));
            if (n > 0) {
                remaining -= n;
            }
            return n;
        }

        @Override
        public void close() {
            // leave the tar stream open
        }
    }
}
