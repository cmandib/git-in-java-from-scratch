package org.example.storage;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.Deflater;

public class ObjectStore {

    public static String store(byte[] content, boolean write) throws Exception {

        // 1. Git blob format
        String header = "blob " + content.length + "\0";

        ByteArrayOutputStream full = new ByteArrayOutputStream();
        full.write(header.getBytes());
        full.write(content);

        byte[] raw = full.toByteArray();

        // 2. SHA-1 hash
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(raw);

        String hash = toHex(digest);

        if (!write) {
            return hash;
        }

        // 3. Compress (zlib)
        byte[] compressed = compress(raw);

        // 4. Write to .git/objects/xx/yyyy...
        String dir = hash.substring(0, 2);
        String file = hash.substring(2);

        Path objectDir = Path.of(".git/objects", dir);
        Files.createDirectories(objectDir);

        Path objectPath = objectDir.resolve(file);

        try (FileOutputStream out = new FileOutputStream(objectPath.toFile())) {
            out.write(compressed);
        }

        return hash;
    }

    private static byte[] compress(byte[] data) {
        try {
            Deflater deflater = new Deflater();
            deflater.setInput(data);
            deflater.finish();

            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                out.write(buffer, 0, count);
            }

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}