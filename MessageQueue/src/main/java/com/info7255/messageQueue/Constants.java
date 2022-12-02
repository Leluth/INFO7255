package com.info7255.messageQueue;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: Constants
 * @date 2022/12/1 22:34
 */
public class Constants {
    // Medical plan related
    public final static String PLAN_TYPE_NAME = "PlanType";
    public final static String ORG_NAME = "_org";
    public final static String CREATION_DATE_NAME = "creationDate";
    public final static String PLAN_JOIN_NAME = "plan_join";
    public final static String PLAN_JOIN_PARENT_NAME = "parent";
    public final static String PLAN_JOIN_RELATIONSHIP_NAME = "name";
    public static final String OBJECT_ID_MAME = "objectId";
    public static final String OBJECT_TYPE_NAME = "objectType";
    public static final String DEDUCTIBLE_NAME = "deductible";
    public static final String COPAY_NAME = "copay";
    public static final String NAME_NAME = "name";
    public static final String PLAN_COST_SHARES_NAME = "planCostShares";
    public static final String LINKED_PLAN_SERVICES_NAME = "linkedPlanServices";
    public static final String LINKED_SERVICE_NAME = "linkedService";
    public static final String PLAN_SERVICE_COST_SHARES_NAME = "planserviceCostShares";
    // Redis configuration related
    public final static String REDIS_URL = "localhost";
    public final static int PORT = 6379;
    // Message queue related
    public static final String MESSAGE_QUEUE_NAME = "MessageQueue";
    public static final String WORKING_QUEUE_NAME = "WorkingQueue";
    public static final String INDEX_NAME = "medical-plan";
    public static final String MESSAGE_FIELD_NAME = "message";
    public static final String OPERATION_FIELD_NAME = "operation";
    public static final String MESSAGE_QUEUE_POST_OPERATION = "post";
    public static final String MESSAGE_QUEUE_DELETE_OPERATION = "delete";
    // Elasticsearch configuration related
    public final static String ES_URL = "localhost";
    public final static int ES_PORT = 9200;
    public final static String ES_SCHEME = "http";
    // Elasticsearch index related
    public final static String INDEX_SHARDS_NUMBER_FIELD_NAME = "index.number_of_shards";
    public final static int INDEX_SHARDS_NUMBER_FIELD_VALUE = 3;
    public final static String INDEX_REPLICAS_NUMBER_FIELD_NAME = "index.number_of_replicas";
    public final static int INDEX_REPLICAS_NUMBER_FIELD_VALUE = 2;
    public static final String MAPPING = "{\n" +
            "\t\"properties\": {\n" +
            "\t\t\"_org\": {\n" +
            "\t\t\t\"type\": \"text\"\n" +
            "\t\t},\n" +
            "\t\t\"objectId\": {\n" +
            "\t\t\t\"type\": \"keyword\"\n" +
            "\t\t},\n" +
            "\t\t\"objectType\": {\n" +
            "\t\t\t\"type\": \"text\"\n" +
            "\t\t},\n" +
            "\t\t\"planType\": {\n" +
            "\t\t\t\"type\": \"text\"\n" +
            "\t\t},\n" +
            "\t\t\"creationDate\": {\n" +
            "\t\t\t\"type\": \"date\",\n" +
            "\t\t\t\"format\" : \"MM-dd-yyyy\"\n" +
            "\t\t},\n" +
            "\t\t\"name\": {\n" +
            "\t\t\t\"type\": \"text\"\n" +
            "\t\t},\n" +
            "\t\t\"copay\": {\n" +
            "\t\t\t\"type\": \"long\"\n" +
            "\t\t},\n" +
            "\t\t\"deductible\": {\n" +
            "\t\t\t\"type\": \"long\"\n" +
            "\t\t},\n" +
            "\t\t\"plan_join\": {\n" +
            "\t\t\t\"type\": \"join\",\n" +
            "\t\t\t\"relations\": {\n" +
            "\t\t\t\t\"medical-plan\": [\"planCostShares\", \"linkedPlanServices\"],\n" +
            "\t\t\t\t\"linkedPlanServices\": [\"linkedService\", \"planserviceCostShares\"]\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";
}
