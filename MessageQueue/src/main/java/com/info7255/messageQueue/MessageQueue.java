package com.info7255.messageQueue;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.xcontent.XContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;

import static com.info7255.messageQueue.Constants.*;

/**
 * @author Shaoshuai Xu
 * @version 1.0
 * @description: MessageQueue
 * @date 2022/12/1 22:33
 */
public class MessageQueue {
    private static final JedisPooled JEDIS = new JedisPooled(REDIS_URL, PORT);
    private static final RestHighLevelClient ES_CLIENT = new RestHighLevelClient(
            RestClient.builder(new HttpHost(ES_URL, ES_PORT, ES_SCHEME)));

    public static void main(String[] args) {
        System.out.println("Message Queue started!");
        try {
            if (!indexExists()) {
                String index = createElasticIndex();
                System.out.println("Elasticsearch Index: " + index + " created!");
            } else {
                System.out.println("Elasticsearch Index existed!");
            }
        } catch (IOException ioException) {
            System.out.println("Errors when indexing:\n" + ioException.getMessage() + "\n");
            return;
        }

        while (true) {
            // Wait for new job
            String jobMessage = JEDIS.rpoplpush(MESSAGE_QUEUE_NAME, WORKING_QUEUE_NAME);
            if (jobMessage == null) {
                continue;
            }
            JSONObject job = new JSONObject(jobMessage);

            // Get message and operation from job
            String message = job.get(MESSAGE_FIELD_NAME).toString();
            String operation = job.get(OPERATION_FIELD_NAME).toString();
            System.out.println("Message Queue received a job!\nOperation: " + operation + "\nMessage:" + message + "\n");

            // Consume Job
            try {
                if (operation.equals(MESSAGE_QUEUE_POST_OPERATION)) {
                    JSONObject plan = new JSONObject(message);
                    String postResult = postDocument(plan, null, INDEX_NAME);
                    System.out.println("Finish a post job! Result: " + postResult + "\n");
                } else {
                    String deleteResult = deleteDocument(message);
                    System.out.println("Finish a delete job! Result: : " + deleteResult + "\n");
                }
            } catch (IOException ioException) {
                System.out.println("Errors when handling job:\n" + ioException.getMessage() + "\n");
                break;
            }
        }
    }

    private static boolean indexExists() throws IOException {
        GetIndexRequest request = new GetIndexRequest(INDEX_NAME);
        return ES_CLIENT.indices().exists(request, RequestOptions.DEFAULT);
    }

    private static String createElasticIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);
        request.settings(Settings.builder()
                .put(INDEX_SHARDS_NUMBER_FIELD_NAME, INDEX_SHARDS_NUMBER_FIELD_VALUE)
                .put(INDEX_REPLICAS_NUMBER_FIELD_NAME, INDEX_REPLICAS_NUMBER_FIELD_VALUE));
        request.mapping(MAPPING, XContentType.JSON);
        return ES_CLIENT.indices().create(request, RequestOptions.DEFAULT).index();
    }

    private static String postDocument(JSONObject jsonObject,
                                       String parentDocumentId, String name) throws IOException {
        StringBuilder response = new StringBuilder();
        if (jsonObject == null) {
            return response.toString();
        }
        // Generate current document
        String documentId = jsonObject.get(OBJECT_ID_MAME).toString();
        JSONObject document = generateDocument(jsonObject, parentDocumentId, name);
        IndexRequest request = new IndexRequest(INDEX_NAME);
        request.id(documentId);
        request.source(document.toString(), XContentType.JSON);
        if (parentDocumentId != null) {
            request.routing(parentDocumentId);
        }
        IndexResponse indexResponse = ES_CLIENT.index(request, RequestOptions.DEFAULT);
        response.append(documentId)
                .append(" ")
                .append(indexResponse.getResult().toString())
                .append("\n");
        // Generate child document
        for (String key : jsonObject.keySet()) {
            if (PLAN_COST_SHARES_NAME.equals(key)) {
                response.append(postDocument(
                        (JSONObject) jsonObject.get(PLAN_COST_SHARES_NAME),
                        documentId,
                        PLAN_COST_SHARES_NAME));
            }
            if (LINKED_SERVICE_NAME.equals(key)) {
                response.append(postDocument(
                        (JSONObject) jsonObject.get(LINKED_SERVICE_NAME),
                        documentId,
                        LINKED_SERVICE_NAME));
            }
            if (PLAN_SERVICE_COST_SHARES_NAME.equals(key)) {
                response.append(postDocument(
                        (JSONObject) jsonObject.get(PLAN_SERVICE_COST_SHARES_NAME),
                        documentId,
                        PLAN_SERVICE_COST_SHARES_NAME));
            }
            if (LINKED_PLAN_SERVICES_NAME.equals(key)) {
                response.append(generateLinkedPlanServices(
                        jsonObject.get(LINKED_PLAN_SERVICES_NAME),
                        documentId));
            }
        }

        return response.toString();
    }

    private static String deleteDocument(String documentId) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX_NAME);
        request.setQuery(QueryBuilders.matchAllQuery());
        BulkByScrollResponse response = ES_CLIENT.deleteByQuery(request, RequestOptions.DEFAULT);
        return documentId + " DELETED";
    }

    private static String generateLinkedPlanServices(Object linkedPlanServices,
                                                     String parentDocumentId) throws IOException {
        StringBuilder response = new StringBuilder();
        if (!(linkedPlanServices instanceof JSONArray)) {
            return response.toString();
        }

        JSONArray jsonArray = (JSONArray) linkedPlanServices;
        for (Object jsonObject : jsonArray) {
            response.append(postDocument((JSONObject) jsonObject, parentDocumentId, LINKED_PLAN_SERVICES_NAME));
        }

        return response.toString();
    }

    private static JSONObject generateDocument(JSONObject jsonObject, String parentDocumentId, String name) {
        Object documentId = jsonObject.get(OBJECT_ID_MAME);
        Object objectType = jsonObject.get(OBJECT_TYPE_NAME);
        Object orgName = jsonObject.get(ORG_NAME);
        JSONObject planJoin = new JSONObject();
        if (parentDocumentId != null) {
            planJoin.put(PLAN_JOIN_PARENT_NAME, parentDocumentId);
        }
        planJoin.put(PLAN_JOIN_RELATIONSHIP_NAME, name);
        JSONObject document = new JSONObject();
        document.put(OBJECT_ID_MAME, documentId.toString());
        document.put(OBJECT_TYPE_NAME, objectType.toString());
        document.put(ORG_NAME, orgName.toString());
        document.put(PLAN_JOIN_NAME, planJoin);

        for (String key : jsonObject.keySet()) {
            if (PLAN_TYPE_NAME.equals(key)) {
                document.put(PLAN_TYPE_NAME, jsonObject.get(PLAN_TYPE_NAME).toString());
            }
            if (CREATION_DATE_NAME.equals(key)) {
                document.put(CREATION_DATE_NAME, jsonObject.get(CREATION_DATE_NAME).toString());
            }
            if (DEDUCTIBLE_NAME.equals(key)) {
                document.put(DEDUCTIBLE_NAME, Long.parseLong(jsonObject.get(DEDUCTIBLE_NAME).toString()));
            }
            if (COPAY_NAME.equals(key)) {
                document.put(COPAY_NAME, Long.parseLong(jsonObject.get(COPAY_NAME).toString()));
            }
            if (NAME_NAME.equals(key)) {
                document.put(NAME_NAME, jsonObject.get(NAME_NAME).toString());
            }
        }

        return document;
    }
}
