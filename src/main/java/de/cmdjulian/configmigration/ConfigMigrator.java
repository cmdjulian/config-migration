package de.cmdjulian.configmigration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
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
import de.cmdjulian.configmigration.utils.JsonPathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConfigMigrator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigMigrator.class);

    private final List<Migration> migrations;
    private final JsonNode configFile;
    private final JsonPath versionSelector;
    private final Integer fallbackVersion;
    private int currentVersion;
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
        this.fallbackVersion = configFileConfig.fallbackVersion();
        this.configFile = configFileConfig.config();
        this.configMapper = configFileConfig.mapper();
        this.currentVersion = resolveCurrentVersion();
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

    private int resolveCurrentVersion() {
        DocumentContext jsonContext = JsonPath.using(jsonPathConfig).parse(configFile);

        try {
            var version = jsonContext.read(versionSelector, int.class);
            logger.debug("found version in config file: {}", version);
            return version;
        } catch (PathNotFoundException e) {
            if (fallbackVersion != null) {
                logger.debug(
                        "could not locate version in config file at {}, falling back to to version {}",
                        versionSelector.getPath(),
                        fallbackVersion
                );
                return fallbackVersion;
            } else {
                logger.debug("could not locate version in config file at {}", versionSelector.getPath());
                throw e;
            }
        }
    }

    /**
     * Extracts the current version of the config file. If a version can't be extracted because the path does not exist
     * and a fallback version is set, the fallback version is returned.
     *
     * @return the current schema version of the config file
     */
    public int currentVersion() {
        return currentVersion;
    }

    /**
     * Runs all migrations on a given config without actually applying it to the config file.
     * This can be used to check if the config file can be migrated in advance.
     */
    public void dryRun() {
        runMigrations(configFile.deepCopy(), null, null);
    }

    /**
     * Runs the migration to the config file.
     */
    public void run() {
        runMigrations(configFile, configFileLocation, configMapper);
    }

    private void runMigrations(JsonNode configFile, Path configFileLocation, ObjectMapper configMapper) {
        var context = JsonPath.using(jsonPathConfig).parse(configFile);
        migrations.stream()
                .filter(migration -> migration.number() > currentVersion)
                .forEach(migration -> {
                    runMigration(context, migration);
                    if (configFileLocation != null && configMapper != null) {
                        try (var out = Files.newOutputStream(configFileLocation)) {
                            configMapper.writeValue(out, context.json());
                        } catch (IOException e) {
                            throw ConfigFileIoException.writeError(configFileLocation, e);
                        }
                    }
                });
    }

    private void runMigration(DocumentContext context, Migration migration) {
        var stepMigrator = new MigrationStepExecutor(context);
        logger.debug("starting migration: [version={}, name={}]", migration.number(), migration.name());
        for (MigrationOperation operation : migration.operations()) {
            if (operation instanceof MigrationOperation.Delete delete) {
                stepMigrator.runDeleteMigration(delete);
            } else if (operation instanceof MigrationOperation.Put put) {
                stepMigrator.runPutMigration(put);
            } else if (operation instanceof MigrationOperation.Rename rename) {
                stepMigrator.runRenameMigration(rename);
            } else if (operation instanceof MigrationOperation.Set set) {
                stepMigrator.runSetMigration(set);
            } else {
                throw new IllegalStateException();
            }
        }

        setCurrentVersionNumber(stepMigrator, migration);
    }

    private void setCurrentVersionNumber(MigrationStepExecutor executor, Migration migration) {
        NumericNode versionNode = JsonNodeFactory.instance.numberNode(migration.number());

        if (executor.pathExists(versionSelector)) {
            logger.debug("updating existing version field on config");
            MigrationOperation.Set operation = new MigrationOperation.Set(versionSelector, versionNode);
            executor.runSetMigration(operation);
        } else {
            logger.debug("putting version field into config");
            var split = JsonPathHelper.extractKeyFromJsonPath(versionSelector);
            MigrationOperation.Put operation = new MigrationOperation.Put(split.path(), split.key(), versionNode);
            executor.runPutMigration(operation);
        }

        this.currentVersion = migration.number();
    }
}