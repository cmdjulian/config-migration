package de.cmdjulian.configmigration.exceptions;

public class MigrationFileReadException extends RuntimeException {
    public MigrationFileReadException(Throwable cause) {
        super("could not read migration files", cause);
    }
}
