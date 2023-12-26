package de.cmdjulian.configmigration;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import de.cmdjulian.configmigration.model.MigrationOperation;
import de.cmdjulian.configmigration.utils.JsonPathHelper;

public class MigrationStepExecutor {

    private final DocumentContext context;

    public MigrationStepExecutor(DocumentContext context) {
        this.context = context;
    }

    public boolean pathExists(JsonPath jsonPath) {
        return JsonPathHelper.pathExists(context, jsonPath);
    }

    public void runSetMigration(MigrationOperation.Set set) {
        if (pathExists(set.path())) {
            context.set(set.path(), set.value());
        } else {
            throw new IllegalArgumentException("value at " + set.path().getPath() + " does not exist and therefore can't be updated");
        }
    }

    public void runRenameMigration(MigrationOperation.Rename rename) {
        if (!pathExists(rename.path())) {
            throw new IllegalArgumentException("value at " + rename.path().getPath() + " does not exist and can not be renamed");
        } else {
            JsonPath jsonPathOldKey = JsonPathHelper.join(rename.path(), rename.oldKey());
            JsonPath jsonPathNewKey = JsonPathHelper.join(rename.path(), rename.newKey());
            if (!pathExists(jsonPathOldKey)) {
                throw new IllegalArgumentException("value at " + jsonPathOldKey.getPath() + " does not exist and can not be renamed");
            }
            if (pathExists(jsonPathNewKey)) {
                throw new IllegalArgumentException("value at " + jsonPathNewKey.getPath() + " exists and can not be used to be renamed to");
            } else {
                context.renameKey(rename.path(), rename.oldKey(), rename.newKey());
            }
        }
    }

    public void runDeleteMigration(MigrationOperation.Delete delete) {
        if (pathExists(delete.path())) {
            context.delete(delete.path());
        } else {
            throw new IllegalArgumentException("value at " + delete.path().getPath() + " does not exist and therefore can't be deleted");
        }
    }

    public void runPutMigration(MigrationOperation.Put put) {
        if (!pathExists(put.path())) {
            throw new IllegalArgumentException("value at " + put.path().getPath() + " does not exist and can not be added");
        } else {
            if (put.key() == null) {
                context.add(put.path(), put.value());
            } else {
                JsonPath jsonPath = JsonPathHelper.join(put.path(), put.key());
                if (pathExists(jsonPath)) {
                    throw new IllegalArgumentException("value at " + jsonPath.getPath() + " already exists and can not be added");
                } else {
                    context.put(put.path(), put.key(), put.value());
                }
            }
        }
    }
}
