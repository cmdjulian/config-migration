package de.cmdjulian.configmigration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import de.cmdjulian.configmigration.config.ConfigFileConfig;
import de.cmdjulian.configmigration.config.MigrationProvider;
import de.cmdjulian.configmigration.exceptions.ConfigFileIoException;
import de.cmdjulian.configmigration.model.Migration;
import de.cmdjulian.configmigration.model.MigrationOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConfigMigrator {

    private final List<Migration> migrations;
    private final JsonNode configFile;
    private final JsonPath versionSelector;
    private Path configFileLocation;
    private ObjectMapper configMapper;
    private Configuration jsonPathConfig = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();

    public ConfigMigrator(@Nonnull ConfigFileConfig config) {
        this(config, new MigrationProvider.ClassPathResourceScanning());
    }

    public ConfigMigrator(@Nonnull ConfigFileConfig configFileConfig, @Nonnull MigrationProvider migrationProvider) {
        this.migrations = migrationProvider.migrations();
        this.configFileLocation = configFileConfig.path();
        this.versionSelector = configFileConfig.versionSelector();
        this.configFile = configFileConfig.config();
        this.configMapper = configFileConfig.mapper();
    }

    @Nonnull
    public List<Migration> getMigrations() {
        return migrations;
    }

    @Nonnull
    public JsonNode getConfigFile() {
        return configFile;
    }

    @Nullable
    public Path getConfigFileLocation() {
        return configFileLocation;
    }

    public void setConfigFileLocation(@Nullable Path configFileLocation) {
        this.configFileLocation = configFileLocation;
    }

    public void setConfigMapper(@Nullable ObjectMapper configMapper) {
        this.configMapper = configMapper;
    }

    public void setJsonPathConfig(Configuration jsonPathConfig) {
        if (!(jsonPathConfig.jsonProvider() instanceof JacksonJsonNodeJsonProvider)) {
            throw new IllegalArgumentException("jsonProvider has to be JacksonJsonNodeJsonProvider");
        }
        if (!(jsonPathConfig.mappingProvider() instanceof JacksonMappingProvider)) {
            throw new IllegalArgumentException("mappingProvider has to be JacksonMappingProvider");
        }

        this.jsonPathConfig = jsonPathConfig;
    }

    public int currentVersion() {
        DocumentContext jsonContext = JsonPath.using(jsonPathConfig).parse(configFile);

        return jsonContext.read(versionSelector, int.class);
    }

    public void dryRun() {
        runMigrations(configFile.deepCopy());
    }

    public void run() {
        var migrated = runMigrations(configFile);
        if (configFileLocation != null && configMapper != null) {
            try (var out = Files.newOutputStream(configFileLocation)) {
                configMapper.writeValue(out, migrated);
            } catch (IOException e) {
                throw ConfigFileIoException.writeError(configFileLocation, e);
            }
        }
    }

    private JsonNode runMigrations(JsonNode jsonNode) {
        int appliedVersion = currentVersion();
        var context = JsonPath.using(jsonPathConfig).parse(jsonNode);
        migrations.stream()
                .filter(migration -> migration.number() > appliedVersion)
                .forEach(migration -> runMigration(context, migration));

        return jsonNode;
    }

    private static boolean pathExists(DocumentContext documentContext, JsonPath jsonPath) {
        try {
            documentContext.read(jsonPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    private static JsonPath joinJsonPaths(String path1, String path2) {
        // Normalizing path1
        if (path1.endsWith(".") || path1.endsWith("[")) {
            path1 = path1.substring(0, path1.length() - 1);
        }

        // Normalizing path2
        if (path2.startsWith(".") || path2.startsWith("[")) {
            path2 = path2.substring(1);
        }

        return JsonPath.compile(path1 + "." + path2);
    }

    private void runMigration(DocumentContext context, Migration migration) {
        for (MigrationOperation operation : migration.operations()) {
            if (operation instanceof MigrationOperation.Delete delete) {
                runDeleteMigration(context, delete);
            } else if (operation instanceof MigrationOperation.Put put) {
                runPutMigration(context, put);
            } else if (operation instanceof MigrationOperation.Rename rename) {
                runRenameMigration(context, rename);
            } else if (operation instanceof MigrationOperation.Set set) {
                runSetMigration(context, set);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private static void runSetMigration(DocumentContext context, MigrationOperation.Set set) {
        if (pathExists(context, set.path())) {
            context.set(set.path(), set.value());
        } else {
            throw new IllegalArgumentException("value at " + set.path().getPath() + " does not exist and therefore can't be updated");
        }
    }

    private static void runRenameMigration(DocumentContext context, MigrationOperation.Rename rename) {
        if (!pathExists(context, rename.path())) {
            throw new IllegalArgumentException("value at " + rename.path().getPath() + " does not exist and can not be renamed");
        } else {
            JsonPath jsonPathOldKey = joinJsonPaths(rename.path().getPath(), rename.oldKey());
            JsonPath jsonPathNewKey = joinJsonPaths(rename.path().getPath(), rename.newKey());
            if (!pathExists(context, jsonPathOldKey)) {
                throw new IllegalArgumentException("value at " + jsonPathOldKey.getPath() + " does not exist and can not be renamed");
            }
            if (pathExists(context, jsonPathNewKey)) {
                throw new IllegalArgumentException("value at " + jsonPathNewKey.getPath() + " exists and can not be used to be renamed to");
            } else {
                context.renameKey(rename.path(), rename.oldKey(), rename.newKey());
            }
        }
    }

    private static void runDeleteMigration(DocumentContext context, MigrationOperation.Delete delete) {
        if (pathExists(context, delete.path())) {
            context.delete(delete.path());
        } else {
            throw new IllegalArgumentException("value at " + delete.path().getPath() + " does not exist and therefore can't be deleted");
        }
    }

    private static void runPutMigration(DocumentContext context, MigrationOperation.Put put) {
        if (!pathExists(context, put.path())) {
            throw new IllegalArgumentException("value at " + put.path().getPath() + " does not exist and can not be added");
        } else {
            JsonPath jsonPath = joinJsonPaths(put.path().getPath(), put.key());
            if (pathExists(context, jsonPath)) {
                throw new IllegalArgumentException("value at " + jsonPath.getPath() + " already exists and can not be added");
            } else {
                context.put(put.path(), put.key(), put.value());
            }
        }
    }
}