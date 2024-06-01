package com.trevorism.service

import com.trevorism.model.Describe
import org.junit.jupiter.api.Test

class InMemoryDescribeServiceTest {

    @Test
    void testDescribe(){
        DescribeService describeService = new InMemoryDescribeService()
        Describe describe = new Describe()
        List<String> result = describeService.describe(describe)
        assert result
        assert result.size() == 4
        assert result.contains("list")
        assert result.contains("create")
        assert result.contains("read")
        assert result.contains("delete")
    }
}
