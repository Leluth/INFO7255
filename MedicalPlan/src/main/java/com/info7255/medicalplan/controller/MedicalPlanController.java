package com.info7255.medicalplan.controller;

import com.info7255.medicalplan.service.AuthorizationService;
import com.info7255.medicalplan.service.MedicalPlanService;
import com.info7255.medicalplan.service.MessageQueueService;
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

import static com.info7255.medicalplan.Constants.Constants.*;

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
    MedicalPlanService medicalPlanService;

    @Autowired
    JsonSchemaValidator jsonSchemaValidator;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private MessageQueueService messageQueueService;

    @GetMapping(value = "/token")
    public ResponseEntity<String> getToken() {
        String token = authorizationService.getToken();
        return new ResponseEntity<>(token, HttpStatus.CREATED);
    }

    @PostMapping(value = "/token")
    public ResponseEntity<String> verifyToken(@RequestHeader HttpHeaders headers) {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(" {\"Message\": \"Token verified!" + "\"}");
    }

    @PostMapping(path = "/plan/", produces = "application/json")
    public ResponseEntity<Object> createMedicalPlan(@RequestBody String plan, @RequestHeader HttpHeaders headers)
            throws Exception {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }

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

        String key = jsonObject.get(OBJECT_TYPE_NAME).toString()
                + PRE_ID_DELIMITER
                + jsonObject.get(OBJECT_ID_MAME).toString();
        if (medicalPlanService.existsRedisKey(key)) {
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
    public ResponseEntity<Object> getMedicalPlan(@RequestHeader HttpHeaders headers, @PathVariable String objectId,
                                                 @PathVariable String objectType) {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }

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
    public ResponseEntity<Object> deleteMedicalPlan(@RequestHeader HttpHeaders headers, @PathVariable String objectId) {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }

        String redisKey = MEDICAL_PLAN_OBJECT_TYPE + PRE_ID_DELIMITER + objectId;
        if (!medicalPlanService.existsRedisKey(redisKey)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject()
                            .put("Warning", "ObjectId doesn't exist!")
                            .toString());
        }

        String receivedETag = headers.getFirst(IF_MATCH_HEADER);
        String actualEtag = medicalPlanService.getMedicalPlanEtag(redisKey);
        if (receivedETag != null && !receivedETag.equals(actualEtag)) {
            return ResponseEntity
                    .status(HttpStatus.PRECONDITION_FAILED)
                    .eTag(actualEtag)
                    .build();
        }

        medicalPlanService.deleteMedicalPlan(redisKey);
        // send delete plan message to MQ
        messageQueueService.publish(objectId, MESSAGE_QUEUE_DELETE_OPERATION);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping(path = "/plan/{objectId}", produces = "application/json")
    public ResponseEntity<Object> patchPlan(@RequestHeader HttpHeaders headers, @RequestBody String medicalPlan,
                                            @PathVariable String objectId) {
        String errorMessage = authorizationService.verifyToken(headers);
        if (errorMessage != null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", errorMessage).toString());
        }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(medicalPlan);
        } catch (JSONException jsonException) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("Error", "Illegal Json data received!").toString());
        }

        String redisKey = MEDICAL_PLAN_OBJECT_TYPE + PRE_ID_DELIMITER + objectId;
        if (!medicalPlanService.existsRedisKey(redisKey)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("Message", "ObjectId does not exist").toString());
        }

        String receivedETag = headers.getFirst(IF_MATCH_HEADER);
        String actualEtag = medicalPlanService.getMedicalPlanEtag(redisKey);
        if (receivedETag != null && !receivedETag.equals(actualEtag)) {
            return ResponseEntity
                    .status(HttpStatus.PRECONDITION_FAILED)
                    .eTag(actualEtag)
                    .build();
        }

        String newEtag = medicalPlanService.saveMedicalPlan(jsonObject, redisKey);
        return ResponseEntity
                .ok()
                .eTag(newEtag)
                .body(new JSONObject().put("Message: ", "Successfully updated medical plan!").toString());
    }
}
