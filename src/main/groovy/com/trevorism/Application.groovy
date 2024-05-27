package com.trevorism

import groovy.transform.CompileStatic
import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OpenAPIDefinition(
        info = @Info(
                title = "Bigquery",
                version = "0.2.0",
                description = "Bigquery API which uses the Trevorism Data API standard",
                contact = @Contact(url = "https://trevorism.com", name = "Trevor Brooks", email = "tbrooks@trevorism.com")
        )
)
@CompileStatic
class Application {
    private static final Logger log = LoggerFactory.getLogger(Application)

    static void main(String[] args) {
        log.info("Started bigquery application")
        Micronaut.run(Application, args)
    }
}
