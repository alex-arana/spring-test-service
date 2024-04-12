package com.arana.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TestSpringServiceApplication

fun main(args: Array<String>) {
    runApplication<TestSpringServiceApplication>(*args)
}
