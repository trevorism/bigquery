package com.trevorism.controller

import com.trevorism.model.Describe
import com.trevorism.service.DescribeService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.inject.Inject


@Controller("/describe")
class DescribeController {

    @Inject
    private DescribeService service

    @Tag(name = "Describe Operations")
    @Operation(summary = "Perform a describe data operation")
    @Post(value = "/", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
    def operate(@Body Describe query) {
        service.describe(query)
    }

    @Tag(name = "Describe Operations")
    @Operation(summary = "Get a description of the performable data actions")
    @Get(value = "{id}", produces = MediaType.APPLICATION_JSON)
    def operateById(String id) {
        service.describe(null)
    }

    @Tag(name = "Describe Operations")
    @Operation(summary = "Get a description of the performable data actions")
    @Get(value = "/", produces = MediaType.APPLICATION_JSON)
    def describe() {
        service.describe(null)
    }
}
