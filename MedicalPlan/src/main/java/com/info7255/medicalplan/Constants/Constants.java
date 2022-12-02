package com.info7255.medicalplan.Constants;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: Constants
 * @date 2022/12/1 22:52
 */
public class Constants {
    // Medical plan related
    public static final String MEDICAL_PLAN_OBJECT_TYPE = "plan";
    public static final String OBJECT_TYPE_NAME = "objectType";
    public static final String OBJECT_ID_MAME = "objectId";
    public static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    public static final String IF_MATCH_HEADER = "If-Match";
    // Token related
    public final static int RSA_KEY_BASE_SIZE = 1024;
    public final static String TOKEN_HEADER = "Authorization";
    public final static String TOKEN_FORMAT = "Bearer ";
    // Redis configuration related
    public final static String REDIS_URL = "localhost";
    public final static int PORT = 6379;
    // Redis key related
    public final static String ETAG_KEY_NAME = "eTag";
    public static final String PRE_ID_DELIMITER = ":";
    public static final String PRE_FIELD_DELIMITER = ">>";
    public static final String REDIS_ALL_PATTERN = "*";
    // Message Queue related
    public static final String MESSAGE_QUEUE_NAME = "MessageQueue";
    public static final String WORKING_QUEUE_NAME = "WorkingQueue";
    public static final String MESSAGE_QUEUE_POST_OPERATION = "post";
    public static final String MESSAGE_QUEUE_DELETE_OPERATION = "delete";
    public static final String MESSAGE_FIELD_NAME = "message";
    public static final String OPERATION_FIELD_NAME = "operation";
}
