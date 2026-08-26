package com.trevorism.service

import com.google.cloud.bigquery.*
import com.google.gson.Gson

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

@jakarta.inject.Singleton
class BigQueryRepository implements DataRepository {

    private static final String DATASET_NAME = "trevorism"
    private static final String GCP_DEFAULT_PROJECT = "trevorism-data"
    private static final String ID_FIELD = "id"
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private static final Gson GSON = new Gson()
    private static final ThreadLocal<SimpleDateFormat> DATE_PARSER = ThreadLocal.withInitial {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT)
        format.setLenient(false)
        return format
    }

    private BigQuery bigquery
    private final Set<String> knownTables = ConcurrentHashMap.newKeySet()

    private BigQuery getClient() {
        if (bigquery == null) {
            bigquery = BigQueryOptions.getDefaultInstance().getService()
        }
        return bigquery
    }

    @Override
    List<String> listTables() {
        DatasetId dataset = DatasetId.of(DATASET_NAME)

        List<String> tableNames = []
        for (Table table : client.listTables(dataset).iterateAll()) {
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

        ensureTableExists(kind, data)

        TableId tableId = TableId.of(DATASET_NAME, kind)
        InsertAllResponse response = client.insertAll(InsertAllRequest.newBuilder(tableId).addRow(toRow(data)).build())

        if (response.hasErrors()) {
            throw new RuntimeException("Failed to insert data: " + response.getInsertErrors())
        }
        return data
    }

    void ensureTableExists(String kind, Map<String, Object> data) {
        if (knownTables.contains(kind)) {
            return
        }

        TableId tableId = TableId.of(DATASET_NAME, kind)
        if (!tableExists(tableId)) {
            Schema schema = inferSchema(data)
            TableDefinition tableDefinition = StandardTableDefinition.of(schema)
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
            createTable(tableInfo)
        }
        knownTables.add(kind)
    }

    protected boolean tableExists(TableId tableId) {
        return client.getTable(tableId) != null
    }

    protected void createTable(TableInfo tableInfo) {
        client.create(tableInfo)
    }

    static Map<String, Object> toRow(Map<String, Object> data) {
        Map<String, Object> row = new LinkedHashMap<>()
        data.each { String key, Object value ->
            row.put(key, normalizeValue(value))
        }
        return row
    }

    static Object normalizeValue(Object value) {
        if (value instanceof Map || value instanceof List) {
            return GSON.toJson(value)
        }
        return value
    }

    static Schema inferSchema(Map<String, Object> data) {
        List<Field> fields = data.collect { String key, Object value ->
            Field.of(key, inferSqlType(normalizeValue(value)))
        }
        return Schema.of(fields)
    }

    static StandardSQLTypeName inferSqlType(Object value) {
        if (value instanceof Boolean) return StandardSQLTypeName.BOOL
        if (value instanceof Integer || value instanceof Long) return StandardSQLTypeName.INT64
        if (value instanceof Double || value instanceof Float) return StandardSQLTypeName.FLOAT64
        if (value instanceof Map || value instanceof List) return StandardSQLTypeName.STRING
        if (isParseableToDate(value)) return StandardSQLTypeName.TIMESTAMP
        return StandardSQLTypeName.STRING
    }

    static boolean isParseableToDate(Object value) {
        if (!(value instanceof CharSequence)) {
            return false
        }

        String text = value.toString()
        if (!text) {
            return false
        }

        ParsePosition position = new ParsePosition(0)
        Date parsed = DATE_PARSER.get().parse(text, position)
        return parsed != null && position.index == text.length()
    }

    @Override
    Map<String, Object> read(String kind, String id) {
        String query = "SELECT * FROM `${DATASET_NAME}.${kind}` WHERE id = \"${id}\""
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build()
        TableResult result = client.query(queryConfig)
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
        client.query(queryConfig)
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
        TableResult result = client.query(queryConfig)

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
