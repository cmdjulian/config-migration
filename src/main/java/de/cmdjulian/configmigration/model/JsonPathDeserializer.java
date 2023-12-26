package de.cmdjulian.configmigration.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;

public class JsonPathDeserializer extends JsonDeserializer<JsonPath> {
    @Override
    public JsonPath deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return JsonPath.compile(p.getValueAsString());
    }
}
