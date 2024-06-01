package com.trevorism.service

import com.google.cloud.bigquery.*

@jakarta.inject.Singleton
class BigQueryRepository implements DataRepository {

    private static final String DATASET_NAME = "trevorism"
    private static final String GCP_DEFAULT_PROJECT = "trevorism-data"
    private static final String ID_FIELD = "id"

    private BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService()

    @Override
    List<String> listTables() {
        DatasetId dataset = DatasetId.of(DATASET_NAME)

        List<String> tableNames = []
        for (Table table : bigquery.listTables(dataset).iterateAll()) {
            tableNames.add(table.getTableId().getTable())
        }
        return tableNames
    }

    @Override
    Map<String, Object> create(String kind, Map<String, Object> data) {
        if (!data.containsKey(ID_FIELD)) {
            String uuid = UUID.randomUUID().toString()
            data.put(ID_FIELD, uuid)
        }

        TableId tableId = TableId.of(DATASET_NAME, kind)
        InsertAllResponse response = bigquery.insertAll(InsertAllRequest.newBuilder(tableId).addRow(data).build())

        if (response.hasErrors()) {
            throw new RuntimeException("Failed to insert data: " + response.getInsertErrors())
        }
        return data
    }

    @Override
    Map<String, Object> read(String kind, String id) {
        String query = "SELECT * FROM `${DATASET_NAME}.${kind}` WHERE id = \"${id}\""
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build()
        TableResult result = bigquery.query(queryConfig)
        return objectFromQueryResult(result)
    }

    @Override
    Map<String, Object> delete(String kind, String id) {
        def result = read(kind, id)
        if (result.isEmpty()) {
            return [:]
        }
        String query = "DELETE FROM `${DATASET_NAME}.${kind}` WHERE id = \"${id}\""
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build()
        bigquery.query(queryConfig)
        return result
    }

    private static Map<String, Object> objectFromQueryResult(TableResult result) {
        Map<String, Object> resultMap = new HashMap<>()
        for (FieldValueList row : result.iterateAll()) {
            for (Field field : result.getSchema().getFields()) {
                resultMap.put(field.getName(), row.get(field.getName()).getValue())
            }
        }

        return resultMap
    }

    @Override
    List<Map<String, Object>> readAll(String kind) {
        String query = "SELECT * FROM `${DATASET_NAME}.${kind}`"
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build()
        TableResult result = bigquery.query(queryConfig)

        return objectListFromQueryResult(result)
    }

    private static List<Map<String, Object>> objectListFromQueryResult(TableResult result) {
        List<Map<String, Object>> resultList = new ArrayList<>()
        for (FieldValueList row : result.iterateAll()) {
            Map<String, Object> resultMap = new HashMap<>()
            for (Field field : result.getSchema().getFields()) {
                resultMap.put(field.getName(), row.get(field.getName()).getValue())
            }
            resultList.add(resultMap)
        }

        return resultList
    }

}
