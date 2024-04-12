package com.arana.demo

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.aws2.s3.AWS2S3Constants
import org.apache.camel.component.jackson.JacksonDataFormat
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

@Component
class SqsConsumerRouteBuilder(
    private val objectMapper: ObjectMapper,
    private val testSpringServiceProperties: TestSpringServiceProperties,
) : RouteBuilder() {
    private val s3BucketName: String
        get() = testSpringServiceProperties.aws.s3BucketName

    private val sqsQueueName: String
        get() = testSpringServiceProperties.aws.sqsQueueName

    private val timestamp: Temporal
        get() = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)

    /**
     * Consumes messages incoming to an AWS SQS queue and uploads the message contents
     * to a target AWS S3 bucket.
     */
    override fun configure() {
        from("aws2-sqs://$sqsQueueName?amazonSQSClient=#sqsClient&deleteIfFiltered=false")
            .log("We have a new quote-of-the-day: \${body}")
            .unmarshal(JacksonDataFormat(objectMapper, QuoteOfDay::class.java))
            .process { exchange ->
                val body = exchange.message.mandatoryBody as QuoteOfDay
                exchange.message.setHeader(AWS2S3Constants.KEY, "qotd-$timestamp")
                exchange.message.body = "${body.author}: ${body.text}"
            }
            .to("aws2-s3://$s3BucketName")
            .log("\${in.header.CamelAwsS3Key} successfully uploaded to S3 bucket '$s3BucketName'")
    }
}
