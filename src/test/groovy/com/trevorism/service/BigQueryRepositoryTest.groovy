package com.trevorism.service

import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import org.junit.jupiter.api.Test

class BigQueryRepositoryTest {

    @Test
    void testInferSqlType_string() {
        assert BigQueryRepository.inferSqlType("hello") == StandardSQLTypeName.STRING
    }

    @Test
    void testInferSqlType_integer() {
        assert BigQueryRepository.inferSqlType(42) == StandardSQLTypeName.INT64
    }

    @Test
    void testInferSqlType_long() {
        assert BigQueryRepository.inferSqlType(100L) == StandardSQLTypeName.INT64
    }

    @Test
    void testInferSqlType_double() {
        assert BigQueryRepository.inferSqlType(3.14d) == StandardSQLTypeName.FLOAT64
    }

    @Test
    void testInferSqlType_float() {
        assert BigQueryRepository.inferSqlType(1.5f) == StandardSQLTypeName.FLOAT64
    }

    @Test
    void testInferSqlType_boolean() {
        assert BigQueryRepository.inferSqlType(true) == StandardSQLTypeName.BOOL
    }

    @Test
    void testInferSqlType_null_fallsBackToString() {
        assert BigQueryRepository.inferSqlType(null) == StandardSQLTypeName.STRING
    }

    @Test
    void testInferSchema_createsCorrectFields() {
        Map<String, Object> data = new LinkedHashMap<>()
        data["id"]     = "abc-123"
        data["name"]   = "widget"
        data["count"]  = 5
        data["price"]  = 9.99d
        data["active"] = true

        Schema schema = BigQueryRepository.inferSchema(data)

        assert schema.getFields().size() == 5

        def fieldMap = schema.getFields().collectEntries { [it.getName(), it.getType().getStandardType()] }
        assert fieldMap["id"] == StandardSQLTypeName.STRING
        assert fieldMap["name"] == StandardSQLTypeName.STRING
        assert fieldMap["count"] == StandardSQLTypeName.INT64
        assert fieldMap["price"] == StandardSQLTypeName.FLOAT64
        assert fieldMap["active"] == StandardSQLTypeName.BOOL
    }

    @Test
    void testInferSqlType_isoDateBecomesTimestamp() {
        assert BigQueryRepository.inferSqlType("2026-08-26T02:36:39Z") == StandardSQLTypeName.TIMESTAMP
    }

    @Test
    void testInferSqlType_nonDateStringsStayStrings() {
        assert BigQueryRepository.inferSqlType("widget") == StandardSQLTypeName.STRING
        assert BigQueryRepository.inferSqlType("0-5-0") == StandardSQLTypeName.STRING
        assert BigQueryRepository.inferSqlType("2026-08-26") == StandardSQLTypeName.STRING
        assert BigQueryRepository.inferSqlType("d3b07384-d9a0-4c9b-b1f2-000000000000") == StandardSQLTypeName.STRING
    }

    @Test
    void testInferSqlType_impossibleDateIsNotATimestamp() {
        assert BigQueryRepository.inferSqlType("2026-13-45T99:99:99Z") == StandardSQLTypeName.STRING
    }

    @Test
    void testInferSqlType_dateWithTrailingGarbageIsNotATimestamp() {
        assert BigQueryRepository.inferSqlType("2026-08-26T02:36:39Zebra") == StandardSQLTypeName.STRING
    }

    @Test
    void testInferSqlType_mapAndListBecomeStrings() {
        assert BigQueryRepository.inferSqlType([nested: "value"]) == StandardSQLTypeName.STRING
        assert BigQueryRepository.inferSqlType(["a", "b"]) == StandardSQLTypeName.STRING
    }

    @Test
    void testNormalizeValue_flattensMapsAndListsToJson() {
        assert BigQueryRepository.normalizeValue([a: 1]) == '{"a":1}'
        assert BigQueryRepository.normalizeValue(["x", "y"]) == '["x","y"]'
    }

    @Test
    void testNormalizeValue_leavesScalarsAlone() {
        assert BigQueryRepository.normalizeValue("hello") == "hello"
        assert BigQueryRepository.normalizeValue(42) == 42
        assert BigQueryRepository.normalizeValue(true) == true
        assert BigQueryRepository.normalizeValue(null) == null
    }

    @Test
    void testToRow_flattensNestedValuesButKeepsScalars() {
        Map<String, Object> data = [id: "abc", detail: [k: "v"], tags: ["a"], count: 3]

        Map<String, Object> row = BigQueryRepository.toRow(data)

        assert row["id"] == "abc"
        assert row["detail"] == '{"k":"v"}'
        assert row["tags"] == '["a"]'
        assert row["count"] == 3
    }

    @Test
    void testToRow_doesNotMutateTheCallerMap() {
        Map<String, Object> data = [detail: [k: "v"]]

        BigQueryRepository.toRow(data)

        assert data["detail"] instanceof Map
    }

    @Test
    void testInferSchema_nestedValueTypedAsStringMatchesTheRow() {
        Map<String, Object> data = [id: "abc", detail: [k: "v"], date: "2026-08-26T02:36:39Z"]

        Schema schema = BigQueryRepository.inferSchema(data)

        def fieldMap = schema.getFields().collectEntries { [it.getName(), it.getType().getStandardType()] }
        assert fieldMap["detail"] == StandardSQLTypeName.STRING
        assert fieldMap["date"] == StandardSQLTypeName.TIMESTAMP
        assert BigQueryRepository.toRow(data)["detail"] instanceof String
    }

    @Test
    void testEnsureTableExists_createsMissingTable() {
        RecordingRepository repository = new RecordingRepository(false)

        repository.ensureTableExists("newkind", [id: "abc", when: "2026-08-26T02:36:39Z"])

        assert repository.created.size() == 1
        assert repository.created[0].getTableId().getTable() == "newkind"
        def fieldMap = repository.created[0].getDefinition().getSchema().getFields()
                .collectEntries { [it.getName(), it.getType().getStandardType()] }
        assert fieldMap["when"] == StandardSQLTypeName.TIMESTAMP
    }

    @Test
    void testEnsureTableExists_doesNotRecreateExistingTable() {
        RecordingRepository repository = new RecordingRepository(true)

        repository.ensureTableExists("existing", [id: "abc"])

        assert repository.created.isEmpty()
    }

    @Test
    void testEnsureTableExists_cachesSoRepeatWritesSkipTheLookup() {
        RecordingRepository repository = new RecordingRepository(true)

        repository.ensureTableExists("cached", [id: "abc"])
        repository.ensureTableExists("cached", [id: "def"])
        repository.ensureTableExists("cached", [id: "ghi"])

        assert repository.lookups == 1
    }

    @Test
    void testEnsureTableExists_cachesPerKind() {
        RecordingRepository repository = new RecordingRepository(true)

        repository.ensureTableExists("kindOne", [id: "abc"])
        repository.ensureTableExists("kindTwo", [id: "abc"])
        repository.ensureTableExists("kindOne", [id: "abc"])

        assert repository.lookups == 2
    }

    @Test
    void testEnsureTableExists_createdTableIsCachedToo() {
        RecordingRepository repository = new RecordingRepository(false)

        repository.ensureTableExists("brandnew", [id: "abc"])
        repository.ensureTableExists("brandnew", [id: "def"])

        assert repository.lookups == 1
        assert repository.created.size() == 1
    }

    private static class RecordingRepository extends BigQueryRepository {
        private final boolean exists
        int lookups = 0
        List<TableInfo> created = []

        RecordingRepository(boolean exists) {
            this.exists = exists
        }

        @Override
        protected void createTable(TableInfo tableInfo) {
            created << tableInfo
        }

        @Override
        protected boolean tableExists(TableId tableId) {
            lookups++
            return exists
        }
    }
}


