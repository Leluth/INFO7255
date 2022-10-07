package com.info7255.medicalplan.controller;

import com.info7255.medicalplan.service.MedicalPlanService;
import com.info7255.medicalplan.util.JsonSchemaValidator;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: MedicalPlanController
 * @date 2022/10/5 22:08
 */
@RestController
@RequestMapping(path = "/medicalplan")
public class MedicalPlanController {
    @Autowired
    MedicalPlanService medicalPlanService ;

    @Autowired
    JsonSchemaValidator jsonSchemaValidator;

    @PostMapping(path = "/plan/", produces = "application/json")
    public ResponseEntity<Object> createMedicalPlan(@RequestBody String plan, @RequestHeader HttpHeaders headers)
            throws Exception {
        if (plan == null || plan.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("Error", "Empty medical plan received!").toString());
        }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(plan);
        } catch (JSONException jsonException) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("Error", "Illegal Json data received!").toString());
        }

        try {
            jsonSchemaValidator.validateJson(jsonObject);
        } catch (ValidationException validationException) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject()
                            .put("JsonSchema Validation Error", validationException.getErrorMessage())
                            .toString());
        }

        String key = jsonObject.get("objectType").toString() + "_" + jsonObject.get("objectId").toString();
        if(medicalPlanService.existsKey(key)){
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new JSONObject()
                            .put("Message", "Medical plan already exists!")
                            .toString());
        }

        String newEtag = medicalPlanService.savePlanToRedis(jsonObject, key);

        return ResponseEntity
                .ok()
                .eTag(newEtag)
                .body(" {\"message\": \"Successfully created medical plan with key: " + key);
    }
}
