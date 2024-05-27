package com.trevorism.controller

import com.trevorism.service.DataRepository
import org.junit.jupiter.api.Test

class ObjectControllerTest {

    @Test
    void testListTables(){
        ObjectController objectController = new ObjectController()
        objectController.repository = [listTables: {-> ["one","two"]} ] as DataRepository
        def tables = objectController.listTables()
        assert tables
        assert tables.size() == 2
        assert tables[0] == "one"
        assert tables[1] == "two"
    }

    @Test
    void testReadAll(){
        ObjectController objectController = new ObjectController()
        objectController.repository = [readAll: { kind-> [[x:kind, y:"z"]]} ] as DataRepository
        def result = objectController.readAll("test")
        assert result
        assert result.size() == 1
        assert result[0].size() == 2
    }

    @Test
    void testRead(){
        ObjectController objectController = new ObjectController()
        objectController.repository = [read: { kind, id -> [x:kind, y:id]} ] as DataRepository
        def result = objectController.read("test", "123-234-345")
        assert result
        assert result.size() == 2
    }

}
