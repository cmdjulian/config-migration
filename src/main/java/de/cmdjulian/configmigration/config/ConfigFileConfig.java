package de.cmdjulian.configmigration.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import de.cmdjulian.configmigration.exceptions.ConfigFileIoException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public interface ConfigFileConfig {

    @Nonnull
    JsonNode config();

    @Nullable
    ObjectMapper mapper();

    @Nullable
    java.nio.file.Path path();

    @Nonnull
    JsonPath versionSelector();

    @Nullable
    Integer fallbackVersion();

    record Path(@Nonnull java.nio.file.Path path, @Nonnull ObjectMapper mapper,
                @Nonnull JsonPath versionSelector, @Nullable Integer fallbackVersion) implements ConfigFileConfig {

        public Path {
            Objects.requireNonNull(path);
            Objects.requireNonNull(mapper);
            Objects.requireNonNull(versionSelector);
        }

        public Path(@Nonnull java.nio.file.Path path, @Nonnull ObjectMapper reader) {
            this(path, reader, null);
        }

        public Path(@Nonnull java.nio.file.Path path, @Nonnull ObjectMapper reader, @Nullable Integer fallbackVersion) {
            this(path, reader, JsonPath.compile("$.version"), fallbackVersion);
        }

        @Nonnull
        @Override
        public JsonNode config() {
            try (var in = Files.newInputStream(path)) {
                return mapper.readTree(in);
            } catch (IOException e) {
                throw ConfigFileIoException.readError(e);
            }
        }
    }

    record Node(@Nonnull JsonNode config, @Nonnull JsonPath versionSelector,
                @Nullable Integer fallbackVersion) implements ConfigFileConfig {

        public Node {
            Objects.requireNonNull(config);
            Objects.requireNonNull(versionSelector);
        }

        public Node(@Nonnull JsonNode config) {
            this(config, null);
        }

        public Node(@Nonnull JsonNode config, @Nullable Integer fallbackVersion) {
            this(config, JsonPath.compile("$.version"), fallbackVersion);
        }

        @Nonnull
        @Override
        public JsonNode config() {
            return config.deepCopy();
        }

        @Nullable
        @Override
        public ObjectMapper mapper() {
            return null;
        }

        @Nullable
        @Override
        public java.nio.file.Path path() {
            return null;
        }
    }
}
