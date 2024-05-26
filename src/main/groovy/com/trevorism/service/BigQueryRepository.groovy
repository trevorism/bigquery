package com.trevorism.service

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetId
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableResult

@jakarta.inject.Singleton
class BigQueryRepository implements DataRepository {

    BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService()

    @Override
    List<String> listTables() {
        DatasetId dataset = DatasetId.of("trevorism")

        List<String> tableNames = []
        for (Table table : bigquery.listTables(dataset).iterateAll()) {
            tableNames.add(table.getTableId().getTable())
        }
        return tableNames
    }

    @Override
    Map<String, Object> create(String kind, Map<String, Object> data) {
        if(!data.containsKey("id")) {
            String uuid = UUID.randomUUID().toString()
            data.put("id", uuid)
        }

        TableId tableId = TableId.of("trevorism", kind)
        InsertAllResponse response = bigquery.insertAll(
                InsertAllRequest.newBuilder(tableId)
                        .addRow(data)
                        .build())

        if (response.hasErrors()) {
            throw new RuntimeException("Failed to insert data: " + response.getInsertErrors())
        }
        return data
    }

    @Override
    Map<String, Object> read(String kind, String id) {
        String query = "SELECT * FROM `trevorism." + kind + "` WHERE id = " + id
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build()
        TableResult result = bigquery.query(queryConfig)
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
        String query = "SELECT * FROM `trevorism." + kind
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build()

        // Run the query using the BigQuery object
        TableResult result = bigquery.query(queryConfig)

        // Convert the result into a list of maps
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
