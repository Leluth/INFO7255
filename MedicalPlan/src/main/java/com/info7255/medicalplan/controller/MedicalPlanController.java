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

import java.util.Map;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: MedicalPlanController
 * @date 2022/10/5 22:08
 */
@RestController
@RequestMapping(path = "/medicalplan")
public class MedicalPlanController {
    private static final String MEDICAL_PLAN_OBJECT_TYPE = "plan";
    private static final String OBJECT_TYPE_NAME = "objectType";
    private static final String OBJECT_ID_MAME = "objectId";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    private static final String PRE_ID_DELIMITER = ":";

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

        String key = jsonObject.get(OBJECT_TYPE_NAME).toString() + PRE_ID_DELIMITER + jsonObject.get(OBJECT_ID_MAME).toString();
        if(medicalPlanService.existsRedisKey(key)){
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new JSONObject()
                            .put("Warning", "Medical plan already exists!")
                            .toString());
        }

        String newEtag = medicalPlanService.saveMedicalPlan(jsonObject, key);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .eTag(newEtag)
                .body(" {\"Message\": \"Successfully created medical plan with key: " + key + "\"}");
    }

    @GetMapping(path = "/{objectType}/{objectId}", produces = "application/json")
    public ResponseEntity<Object> getPlan(@RequestHeader HttpHeaders headers, @PathVariable String objectId,
                                          @PathVariable String objectType) {
        String redisKey = objectType + PRE_ID_DELIMITER + objectId;
        if (!medicalPlanService.existsRedisKey(redisKey)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject()
                            .put("Error", "ObjectId doesn't exist!")
                            .toString());
        }

        String receivedETag = headers.getFirst(IF_NONE_MATCH_HEADER);
        String actualEtag = "";
        if (objectType.equals(MEDICAL_PLAN_OBJECT_TYPE)) {
            actualEtag = medicalPlanService.getMedicalPlanEtag(redisKey);
            if (receivedETag != null && receivedETag.equals(actualEtag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(actualEtag)
                        .body(" {\"Message\": \"Medical plan hasn't been modified!" + "\"}");
            }
        }

        Map<String, Object> plan = medicalPlanService.getMedicalPlan(redisKey);
        return ResponseEntity
                .ok()
                .eTag(actualEtag)
                .body(new JSONObject(plan).toString());
    }

    @DeleteMapping(path = "/plan/{objectId}", produces = "application/json")
    public ResponseEntity<Object> getPlan(@RequestHeader HttpHeaders headers, @PathVariable String objectId){
        String redisKey = MEDICAL_PLAN_OBJECT_TYPE + PRE_ID_DELIMITER + objectId;
        if (!medicalPlanService.existsRedisKey(redisKey)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject()
                            .put("Warning", "ObjectId doesn't exist!")
                            .toString());
        }

        medicalPlanService.deleteMedicalPlan(redisKey);

        return ResponseEntity
                .noContent()
                .build();
    }
}
