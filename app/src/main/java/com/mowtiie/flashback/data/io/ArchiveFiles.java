package com.mowtiie.flashback.data.io;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.WorkerThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Reads and writes archive text through a content Uri from the Storage Access
 * Framework. SAF is used so the app needs no storage permission and works the
 * same on every supported Android version.
 */
public final class ArchiveFiles {

    /** Guards against a "file" that is actually a multi-gigabyte blob. */
    private static final long MAX_BYTES = 16L * 1024 * 1024;

    private ArchiveFiles() {
    }

    @WorkerThread
    public static void write(ContentResolver resolver, Uri uri, String content)
            throws IOException {
        try (OutputStream out = resolver.openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("Could not open the chosen location for writing.");
            }
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            writer.write(content);
            writer.flush();
        }
    }

    @WorkerThread
    public static String read(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) {
                throw new IOException("Could not open the chosen file.");
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            long total = 0;
            int read;
            while ((read = reader.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new IOException("This file is too large to be a Flashback export.");
                }
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
    }
}
