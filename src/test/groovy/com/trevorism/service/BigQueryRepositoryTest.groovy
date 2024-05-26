package com.trevorism.service

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetInfo
import org.junit.jupiter.api.Test

import java.sql.Timestamp
import java.time.Instant

class BigQueryRepositoryTest {

    @Test
    void testCreate() {

        //BigQuery bigquery = BigQueryOptions.newBuilder().build().getService()
        //DatasetInfo datasetInfo = DatasetInfo.newBuilder("trevorism").build()

        BigQueryRepository bigQueryRepository = new BigQueryRepository()

        Map<String, Object> data = new HashMap<>()
        data.put("name", "Laila")
        data.put("age", 44)
        data.put("active", true)
        data.put("created", Instant.now().toString())

        bigQueryRepository.create("users", data)



    }
}
