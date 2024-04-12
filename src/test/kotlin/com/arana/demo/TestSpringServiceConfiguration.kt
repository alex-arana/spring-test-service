package com.arana.demo

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails
import org.apache.camel.impl.engine.DefaultShutdownStrategy
import org.apache.camel.spi.CamelContextCustomizer
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.providers.AwsRegionProvider
import software.amazon.awssdk.services.sqs.SqsClient
import java.util.Optional
import java.util.concurrent.TimeUnit

@Configuration
class TestSpringServiceConfiguration {

    @Bean
    @ServiceConnection
    fun localStackContainer(): LocalStackContainer {
        val dockerImageName = DockerImageName.parse("localstack/localstack:3.2.0")
        return LocalStackContainer(dockerImageName)
            .withStdOutConsumer()
            .withServices(
                LocalStackContainer.Service.S3,
                LocalStackContainer.Service.SNS,
                LocalStackContainer.Service.SQS
            )
    }

    @Bean
    fun sqsClient(
        credentials: AwsCredentialsProvider,
        awsRegionProvider: AwsRegionProvider,
        optionalAwsConnectionDetails: Optional<AwsConnectionDetails>,
    ): SqsClient {
        val awsConnectionDetails = optionalAwsConnectionDetails.orElseThrow()
        return SqsClient.builder()
            .endpointOverride(awsConnectionDetails.endpoint)
            .credentialsProvider(credentials)
            .region(awsRegionProvider.region)
            .build()
    }

    /**
     * Shutdown all Camel routes swiftly after integration tests.
     */
    @Bean
    fun camelContextCustomizer() = CamelContextCustomizer { camelContext ->
        camelContext.shutdownStrategy = DefaultShutdownStrategy().apply {
            timeout = 1
            timeUnit = TimeUnit.SECONDS
            isSuppressLoggingOnTimeout = true
            isShutdownNowOnTimeout = true
        }
    }
}
