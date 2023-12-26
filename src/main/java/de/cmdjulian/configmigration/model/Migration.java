package de.cmdjulian.configmigration.model;

import javax.annotation.Nonnull;
import java.util.List;

public record Migration(int number, @Nonnull String name, @Nonnull List<MigrationOperation> operations) {
}
