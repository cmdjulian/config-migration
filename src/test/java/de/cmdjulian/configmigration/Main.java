package de.cmdjulian.configmigration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.secretx33.resourceresolver.PathMatchingResourcePatternResolver;
import de.cmdjulian.configmigration.config.ConfigFileConfig;
import de.cmdjulian.configmigration.model.Migration;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        var mapper = new ObjectMapper(new YAMLFactory());
        var configFile = mapper.readTree(resolver.getResource("classpath:config.yaml").getInputStream());
        var migrator = new ConfigMigrator(new ConfigFileConfig.Node(configFile, 0));
        System.out.println("current version: " + migrator.currentVersion());
        for (Migration migration : migrator.getMigrations()) {
            System.out.println(migration.toString());
        }

        System.out.println("config before migration: " + migrator.getConfigFile());
        migrator.run();
        System.out.println("config after migration: " + migrator.getConfigFile());
        System.out.println("current version after migration: " + migrator.currentVersion());
    }
}
