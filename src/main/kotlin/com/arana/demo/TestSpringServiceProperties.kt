package com.arana.demo

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("test-spring-service")
class TestSpringServiceProperties {
    @Valid
    val aws = AwsProperties()

    class AwsProperties {
        @NotBlank
        lateinit var s3BucketName: String

        @NotBlank
        lateinit var snsTopicName: String

        @NotBlank
        lateinit var sqsQueueName: String
    }
}
