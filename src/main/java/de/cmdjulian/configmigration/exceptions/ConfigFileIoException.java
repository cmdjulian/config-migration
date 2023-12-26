package de.cmdjulian.configmigration.exceptions;

import java.nio.file.Path;

public class ConfigFileIoException extends RuntimeException {
    ConfigFileIoException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ConfigFileIoException readError(Throwable cause) {
        return new ConfigFileIoException("could not read config file", cause);
    }

    public static ConfigFileIoException writeError(Path location, Throwable cause) {
        return new ConfigFileIoException("could not write config file to " + location, cause);
    }
}
