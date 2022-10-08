package com.info7255.medicalplan.service;

import com.info7255.medicalplan.dao.MedicalPlanDAO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: MedicalPlanService
 * @date 2022/10/6 22:25
 */
@Service
public class MedicalPlanService {
    private final static String ETAG_KEY_NAME = "eTag";
    private static final String PRE_ID_DELIMITER = ":";
    private static final String PRE_FIELD_DELIMITER = ">>";
    private static final String REDIS_ALL_PATTERN = "*";
    private static final String OBJECT_TYPE_NAME = "objectType";
    private static final String OBJECT_ID_MAME = "objectId";

    @Autowired
    MedicalPlanDAO medicalPlanDAO;

    public String saveMedicalPlan(JSONObject planObject, String key) {
        Map<String, Object> saveMedicalPlanMap = saveMedicalPlanToRedis(key, planObject);

        String saveMedicalPlanString = new JSONObject(saveMedicalPlanMap).toString();
        String newEtag = DigestUtils.md5DigestAsHex(saveMedicalPlanString.getBytes(StandardCharsets.UTF_8));
        medicalPlanDAO.hSet(key, ETAG_KEY_NAME, newEtag);

        return newEtag;
    }

    public Map<String, Object> saveMedicalPlanToRedis(String key, JSONObject planObject){
        saveJSONObjectToRedis(planObject);
        return getMedicalPlan(key);
    }

    public Map<String, Object> getMedicalPlan(String redisKey) {
        Map<String, Object> outputMap = new HashMap<>();

        Set<String> keys = medicalPlanDAO.getKeysByPattern(redisKey + REDIS_ALL_PATTERN);
        for (String key : keys) {
            if (key.equals(redisKey)) {
                Map<String, String> value = medicalPlanDAO.hGetAll(key);
                for (String name : value.keySet()) {
                    if (!name.equalsIgnoreCase(ETAG_KEY_NAME)) {
                        outputMap.put(name,
                                isDouble(value.get(name)) ? Double.parseDouble(value.get(name)) : value.get(name));
                    }
                }
            } else {
                String newKey = key.substring((redisKey + PRE_FIELD_DELIMITER).length());
                Set<String> members = medicalPlanDAO.sMembers(key);
                if (members.size() > 1) {
                    List<Object> listObj = new ArrayList<>();
                    for (String member : members) {
                        listObj.add(getMedicalPlan(member));
                    }
                    outputMap.put(newKey, listObj);
                } else {
                    Map<String, String> val = medicalPlanDAO.hGetAll(members.iterator().next());
                    Map<String, Object> newMap = new HashMap<>();
                    for (String name : val.keySet()) {
                        newMap.put(name,
                                isDouble(val.get(name)) ? Double.parseDouble(val.get(name)) : val.get(name));
                    }
                    outputMap.put(newKey, newMap);
                }
            }
        }

        return outputMap;
    }

    public void deleteMedicalPlan(String redisKey) {
        Set<String> keys = medicalPlanDAO.getKeysByPattern(redisKey + REDIS_ALL_PATTERN);
        for (String key : keys) {
            if (key.equals(redisKey)) {
                medicalPlanDAO.deleteKeys(new String[] {key});
            } else {
                Set<String> members = medicalPlanDAO.sMembers(key);
                if (members.size() > 1) {
                    for (String member : members) {
                        deleteMedicalPlan(member);
                    }
                    medicalPlanDAO.deleteKeys(new String[] {key});
                } else {
                    medicalPlanDAO.deleteKeys(new String[]{members.iterator().next(), key});
                }
            }
        }
    }

    public boolean existsRedisKey(String key){
        return medicalPlanDAO.existsKey(key);
    }

    public String getMedicalPlanEtag(String key) {
        return medicalPlanDAO.hGet(key, ETAG_KEY_NAME);
    }

    private Map<String, Map<String, Object>> saveJSONObjectToRedis(JSONObject object) {
        Map<String, Map<String, Object>> redisKeyMap = new HashMap<>();
        Map<String, Object> objectFieldMap = new HashMap<>();

        String redisKey = object.get(OBJECT_TYPE_NAME) + PRE_ID_DELIMITER + object.get(OBJECT_ID_MAME);
        for (String field : object.keySet()) {
            Object value = object.get(field);
            if (value instanceof JSONObject) {
                Map<String, Map<String, Object>> convertedValue = saveJSONObjectToRedis((JSONObject) value);
                medicalPlanDAO.sadd(redisKey + PRE_FIELD_DELIMITER + field,
                        convertedValue.entrySet().iterator().next().getKey());
            } else if (value instanceof JSONArray) {
                List<Map<String, Map<String, Object>>> convertedValue = saveJSONArrayToRedis((JSONArray) value);
                for (Map<String, Map<String, Object>> entry : convertedValue) {
                    for (String listKey : entry.keySet()) {
                        medicalPlanDAO.sadd(redisKey + PRE_FIELD_DELIMITER + field, listKey);
                    }
                }
            } else {
                medicalPlanDAO.hSet(redisKey, field, value.toString());
                objectFieldMap.put(field, value);
                redisKeyMap.put(redisKey, objectFieldMap);
            }
        }

        return redisKeyMap;
    }

    private List<Map<String, Map<String, Object>>> saveJSONArrayToRedis(JSONArray array) {
        List<Map<String, Map<String, Object>>> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                List<Map<String, Map<String, Object>>> convertedValue = saveJSONArrayToRedis((JSONArray) value);
                list.addAll(convertedValue);
            } else if (value instanceof JSONObject) {
                Map<String, Map<String, Object>> convertedValue = saveJSONObjectToRedis((JSONObject) value);
                list.add(convertedValue);
            }
        }
        return list;
    }

    private boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException numberFormatException) {
            return false;
        }
    }
}
