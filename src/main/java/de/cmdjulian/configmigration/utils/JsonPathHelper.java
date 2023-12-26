package de.cmdjulian.configmigration.utils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for JsonPath operations.
 */
public class JsonPathHelper {
    private JsonPathHelper() {
    }

    public record KeyAndPath(JsonPath path, String key) {
    }

    /**
     * Extracts the last component from a JsonPath and returns the split data. Only simple paths are supported
     * like '$.version' or '$['version']'. For instance the path '$.version' gets split into ['$' and 'version'].
     *
     * @param jsonPath the path to split
     * @return the last path component as key and the rest of the path.
     */
    public static KeyAndPath extractKeyFromJsonPath(JsonPath jsonPath) {
        var jsonPathString = jsonPath.getPath();
        Matcher matcher = Pattern.compile("'(.*?)'|\"(.*?)\"|\\['(.*?)']|(\\w+)").matcher(jsonPathString);

        List<String> keys = new ArrayList<>();
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String key = matcher.group(i);
                if (key == null || key.matches("\\d+")) {
                    continue;
                }
                keys.add(key);
            }
        }

        String key = keys.remove(keys.size() - 1);
        JsonPath path;
        if (keys.isEmpty()) {
            path = JsonPath.compile("$");
        } else {
            path = JsonPath.compile(String.join(".", keys));
        }

        return new KeyAndPath(path, key);
    }

    /**
     * Check weather a given json path exists in a DocumentContext by evaluating if it points to an existing entry.
     *
     * @param documentContext the context to check against
     * @param jsonPath        the json path to evaluate
     * @return true if exists and points to a valid entry, else false
     */
    public static boolean pathExists(DocumentContext documentContext, JsonPath jsonPath) {
        try {
            documentContext.read(jsonPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Joins provided path with the provided components. Joining '$.foo' with 'bar' results in '$.foo.bar'.
     *
     * @param jsonPath   root path to join up on
     * @param components components which are joined in the provided order
     * @return the composed json path.
     */
    public static JsonPath join(JsonPath jsonPath, String... components) {
        StringBuilder builder = new StringBuilder(jsonPath.getPath());
        for (String path : components) {
            if (builder.toString().endsWith(".") || builder.toString().endsWith("[")) {
                builder = new StringBuilder(builder.substring(0, builder.length() - 1));
            }
            if (path.startsWith(".") || path.startsWith("[")) {
                path = path.substring(1);
            }

            builder.append(".").append(path);
        }

        return JsonPath.compile(builder.toString());
    }
}
