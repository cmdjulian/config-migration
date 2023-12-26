package de.cmdjulian.configmigration.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.jayway.jsonpath.JsonPath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MigrationOperation.Delete.class, name = "delete"),
        @JsonSubTypes.Type(value = MigrationOperation.Put.class, name = "put"),
        @JsonSubTypes.Type(value = MigrationOperation.Rename.class, name = "rename"),
        @JsonSubTypes.Type(value = MigrationOperation.Set.class, name = "set"),
})
public sealed interface MigrationOperation {
    record Delete(
            @JsonDeserialize(using = JsonPathDeserializer.class) @Nonnull JsonPath path) implements MigrationOperation {
        public Delete {
            Objects.requireNonNull(path);
        }

        @Override
        public String toString() {
            return "Delete[path=" + path.getPath() + ']';
        }
    }

    record Put(@JsonDeserialize(using = JsonPathDeserializer.class) @Nonnull JsonPath path, @Nullable String key,
               @Nonnull JsonNode value) implements MigrationOperation {
        public Put {
            Objects.requireNonNull(path);
            Objects.requireNonNull(value);
        }

        @Override
        public String toString() {
            return "Put[path=" + path.getPath() + ", name=" + key + ", value=" + value + ']';
        }
    }

    record Rename(@JsonDeserialize(using = JsonPathDeserializer.class) @Nonnull JsonPath path, @Nonnull String oldKey,
                  @Nonnull String newKey) implements MigrationOperation {
        public Rename {
            Objects.requireNonNull(path);
            Objects.requireNonNull(oldKey);
            Objects.requireNonNull(newKey);
        }

        @Override
        public String toString() {
            return "Rename[path=" + path.getPath() + ", oldKey=" + oldKey + ", newKey=" + newKey + ']';
        }
    }

    record Set(@JsonDeserialize(using = JsonPathDeserializer.class) @Nonnull JsonPath path,
               @Nonnull JsonNode value) implements MigrationOperation {
        public Set {
            Objects.requireNonNull(path);
            Objects.requireNonNull(value);
        }

        @Override
        public String toString() {
            return "Set[path=" + path.getPath() + ", value=" + value + ']';
        }
    }
}
