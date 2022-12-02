package com.info7255.medicalplan.service;

import com.info7255.medicalplan.dao.MedicalPlanDAO;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.info7255.medicalplan.Constants.Constants.*;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: MessageQueueService
 * @date 2022/12/1 21:17
 */
@Service
public class MessageQueueService {
    @Autowired
    MedicalPlanDAO medicalPlanDAO;

    public void publish(String message, String operation) {
        JSONObject object = new JSONObject();
        object.put(MESSAGE_FIELD_NAME, message);
        object.put(OPERATION_FIELD_NAME, operation);
        // send message to redis queue
        medicalPlanDAO.lpush(MESSAGE_QUEUE_NAME, object.toString());
    }
}
