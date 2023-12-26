package de.cmdjulian.configmigration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.cmdjulian.configmigration.exceptions.MigrationFileReadException;
import io.github.secretx33.resourceresolver.PathMatchingResourcePatternResolver;
import io.github.secretx33.resourceresolver.Resource;
import io.github.secretx33.resourceresolver.ResourcePatternResolver;
import de.cmdjulian.configmigration.model.Migration;
import de.cmdjulian.configmigration.model.MigrationOperation;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MigrationProvider {

    List<Migration> migrations();

    record ClassPathResourceScanning(@Nonnull String location, @Nonnull String extension,
                                     @Nonnull ObjectMapper mapper) implements MigrationProvider {
        public ClassPathResourceScanning {
            Objects.requireNonNull(location);
            Objects.requireNonNull(extension);
            Objects.requireNonNull(mapper);
        }

        public ClassPathResourceScanning() {
            this("classpath:migrations/*.yaml", "yaml", new ObjectMapper(new YAMLFactory()));
        }

        @Override
        public List<Migration> migrations() {
            var migrationFilePattern = Pattern.compile("^V([0-9_]+)__(.*)\\." + extension + "$");
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources;

            try {
                resources = resolver.getResources(location);
            } catch (IOException e) {
                throw new MigrationFileReadException(e);
            }

            return Arrays.stream(resources)
                    .filter(migration -> migrationFilePattern.asMatchPredicate().test(migration.getFilename()))
                    .map(resource -> readMigration(migrationFilePattern, resource))
                    .sorted(Comparator.comparing(Migration::number))
                    .toList();
        }

        @SuppressWarnings({"DataFlowIssue", "ResultOfMethodCallIgnored"})
        private Migration readMigration(Pattern migrationFilePattern, Resource resource) {
            Matcher matcher = migrationFilePattern.matcher(resource.getFilename());
            matcher.find();

            int number = Integer.parseInt(matcher.group(1).replace("_", "."));
            String description = matcher.group(2);

            var typeToken = TypeFactory.defaultInstance().constructCollectionType(List.class, MigrationOperation.class);
            List<MigrationOperation> operations;
            try {
                operations = mapper.readValue(resource.getInputStream(), typeToken);
            } catch (IOException e) {
                throw new MigrationFileReadException(e);
            }

            return new Migration(number, description, operations);
        }
    }

    record Migrations(@Nonnull List<Migration> migrations) implements MigrationProvider {
        public Migrations {
            Objects.requireNonNull(migrations);
        }
    }
}
