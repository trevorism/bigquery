package com.trevorism.service

import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
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
}


