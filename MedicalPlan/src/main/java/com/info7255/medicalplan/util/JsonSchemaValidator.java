package com.info7255.medicalplan.util;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: JsonSchemaValidator
 * @date 2022/10/5 22:10
 */
@Service
public class JsonSchemaValidator {
    public void validateJson(JSONObject object) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/JsonSchema.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(object);
        }
    }
}
