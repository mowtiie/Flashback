package com.mowtiie.flashback.data.io;

/**
 * Thrown when a file cannot be read as a valid archive. The message is written
 * to be shown to the user directly, so it names what was wrong and where.
 */
public class ArchiveException extends Exception {

    public ArchiveException(String message) {
        super(message);
    }

    public ArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
