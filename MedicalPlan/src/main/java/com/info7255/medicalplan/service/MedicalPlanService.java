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

    @Autowired
    MedicalPlanDAO medicalPlanDAO;

    public String savePlanToRedis(JSONObject planObject, String key) {
        Map<String, Object> saveMedicalPlanMap = saveMedicalPlan(key, planObject);
        String saveMedicalPlanString = new JSONObject(saveMedicalPlanMap).toString();
        String newEtag = DigestUtils.md5DigestAsHex(saveMedicalPlanString.getBytes(StandardCharsets.UTF_8));
        medicalPlanDAO.hSet(key, ETAG_KEY_NAME, newEtag);
        return newEtag;
    }

    public Map<String, Object> saveMedicalPlan(String key, JSONObject planObject){
        convertJSONObjectToMap(planObject);
        Map<String, Object> outputMap = new HashMap<>();
        getOrDeleteData(key, outputMap, false);
        return outputMap;
    }

    private Map<String, Map<String, Object>> convertJSONObjectToMap(JSONObject object) {
        Map<String, Map<String, Object>> redisKeyMap = new HashMap<>();
        Map<String, Object> objectFieldMap = new HashMap<>();

        String redisKey = object.get("objectType") + "_" + object.get("objectId");
        for (String field : object.keySet()) {
            Object value = object.get(field);
            if (value instanceof JSONObject) {
                Map<String, Map<String, Object>> convertedValue = convertJSONObjectToMap((JSONObject) value);
                medicalPlanDAO.sadd(redisKey + "_" + field, convertedValue.entrySet().iterator().next().getKey());
            } else if (value instanceof JSONArray) {
                List<Map<String, Map<String, Object>>> convertedValue = convertJSONArrayToList((JSONArray) value);
                for (Map<String, Map<String, Object>> entry : convertedValue) {
                    for (String listKey : entry.keySet()) {
                        medicalPlanDAO.sadd(redisKey + "_" + field, listKey);
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

    private List<Map<String, Map<String, Object>>> convertJSONArrayToList(JSONArray array) {
        List<Map<String, Map<String, Object>>> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                List<Map<String, Map<String, Object>>> convertedValue = convertJSONArrayToList((JSONArray) value);
                list.addAll(convertedValue);
            } else if (value instanceof JSONObject) {
                Map<String, Map<String, Object>> convertedValue = convertJSONObjectToMap((JSONObject) value);
                list.add(convertedValue);
            }
        }
        return list;
    }

    private Map<String, Object> getOrDeleteData(String redisKey, Map<String, Object> outputMap, boolean isDelete) {
        Set<String> keys = medicalPlanDAO.getKeysByPattern(redisKey + "*");
        for (String key : keys) {
            if (key.equals(redisKey)) {
                if (isDelete) {
                    medicalPlanDAO.deleteKeys(new String[] {key});
                } else {
                    Map<String, String> val = medicalPlanDAO.hGetAll(key);
                    for (String name : val.keySet()) {
                        if (!name.equalsIgnoreCase(ETAG_KEY_NAME)) {
                            outputMap.put(name,
                                    isDouble(val.get(name)) ? Double.parseDouble(val.get(name)) : val.get(name));
                        }
                    }
                }
            } else {
                String newStr = key.substring((redisKey + "_").length());
                Set<String> members = medicalPlanDAO.sMembers(key);
                if (members.size() > 1) {
                    List<Object> listObj = new ArrayList<>();
                    for (String member : members) {
                        if (isDelete) {
                            getOrDeleteData(member, null, true);
                        } else {
                            Map<String, Object> listMap = new HashMap<>();
                            listObj.add(getOrDeleteData(member, listMap, false));
                        }
                    }
                    if (isDelete) {
                        medicalPlanDAO.deleteKeys(new String[] {key});
                    } else {
                        outputMap.put(newStr, listObj);
                    }
                } else {
                    if (isDelete) {
                        medicalPlanDAO.deleteKeys(new String[]{members.iterator().next(), key});
                    } else {
                        Map<String, String> val = medicalPlanDAO.hGetAll(members.iterator().next());
                        Map<String, Object> newMap = new HashMap<>();
                        for (String name : val.keySet()) {
                            newMap.put(name,
                                    isDouble(val.get(name)) ? Double.parseDouble(val.get(name)) : val.get(name));
                        }
                        outputMap.put(newStr, newMap);
                    }
                }
            }
        }
        return outputMap;
    }

    public boolean existsKey(String key){
        return medicalPlanDAO.existsKey(key);
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
