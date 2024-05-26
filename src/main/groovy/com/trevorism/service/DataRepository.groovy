package com.trevorism.service

interface DataRepository {

    List<String> listTables()
    Map<String, Object> create(String kind, Map<String, Object> data)
    Map<String, Object> read(String kind, String id)
    List<Map<String, Object>> readAll(String kind)
}