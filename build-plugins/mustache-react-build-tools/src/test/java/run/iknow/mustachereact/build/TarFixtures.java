package run.iknow.mustachereact.build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/** Writes small tar.gz archives for tests, including entries a real tool would refuse to create. */
final class TarFixtures {

    private final ByteArrayOutputStream tar = new ByteArrayOutputStream();

    TarFixtures file(String name, String content, int mode) throws IOException {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        header(name, mode, data.length, '0', "");
        tar.write(data);
        pad(data.length);
        return this;
    }

    TarFixtures longFile(String name, String content) throws IOException {
        byte[] path = (name).getBytes(StandardCharsets.UTF_8);
        String record = " path=" + name + "\n";
        int length = record.getBytes(StandardCharsets.UTF_8).length;
        length += String.valueOf(length + String.valueOf(length).length()).length();
        byte[] pax = (length + record).getBytes(StandardCharsets.UTF_8);
        header("PaxHeader", 0644, pax.length, 'x', "");
        tar.write(pax);
        pad(pax.length);
        return file(name.substring(0, Math.min(name.length(), 99)), content, 0644).noop(path);
    }

    TarFixtures symlink(String name, String target) throws IOException {
        header(name, 0777, 0, '2', target);
        return this;
    }

    private TarFixtures noop(byte[] ignored) {
        return this;
    }

    private void header(String name, int mode, long size, char type, String link) throws IOException {
        byte[] h = new byte[512];
        put(h, 0, name, 100);
        put(h, 100, String.format("%07o", mode), 8);
        put(h, 108, "0000000", 8);
        put(h, 116, "0000000", 8);
        put(h, 124, String.format("%011o", size), 12);
        put(h, 136, "00000000000", 12);
        h[156] = (byte) type;
        put(h, 157, link, 100);
        put(h, 257, "ustar", 6);
        put(h, 263, "00", 2);
        for (int i = 148; i < 156; i++) {
            h[i] = ' ';
        }
        int sum = 0;
        for (byte b : h) {
            sum += b & 0xff;
        }
        put(h, 148, String.format("%06o", sum), 7);
        tar.write(h);
    }

    private void pad(long size) {
        long padding = (512 - size % 512) % 512;
        for (int i = 0; i < padding; i++) {
            tar.write(0);
        }
    }

    private static void put(byte[] header, int offset, String value, int length) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes, 0, header, offset, Math.min(bytes.length, length));
    }

    Path writeTo(Path file) throws IOException {
        tar.write(new byte[1024]);
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
            out.write(tar.toByteArray());
        }
        return file;
    }
}
