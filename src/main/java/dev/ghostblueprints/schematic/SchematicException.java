package dev.ghostblueprints.schematic;

/** Thrown when a schematic file cannot be read or is not supported. */
public final class SchematicException extends Exception {
    public SchematicException(String message) {
        super(message);
    }

    public SchematicException(String message, Throwable cause) {
        super(message, cause);
    }
}
