package com.arana.demo

import org.apache.camel.Produce
import org.apache.camel.ProducerTemplate
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Durations
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestSpringServiceApplicationTests : AbstractSpringIntegrationTest() {
    @Produce("direct:sns-producer-route")
    private lateinit var snsProducerTemplate: ProducerTemplate

    @BeforeEach
    fun setUp() {
        addTopicSubscriber(testSpringServiceProperties.aws.sqsQueueName)
    }

    @Test
    fun `Publishing message to an SNS topic uploads a file to a target S3 bucket`() {
        val quoteOfDay = QuoteOfDay(
            author = "Arthur C. Clarke",
            text = "Any sufficiently advanced technology is indistinguishable from magic."
        )

        snsProducerTemplate.sendBody(quoteOfDay)

        val actual = await atMost Durations.TEN_SECONDS untilCallTo {
            readS3Object(testSpringServiceProperties.aws.s3BucketName)
        } matches {
            !it.isNullOrBlank()
        }
        assertThat(actual).matches("^${quoteOfDay.author}: ${quoteOfDay.text}\$")
    }
}
