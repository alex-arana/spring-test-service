package com.arana.demo

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.dataformat.JsonLibrary
import org.springframework.stereotype.Component

/**
 * Route that serialises an incoming message and publishes it to an AWS SNS topic.
 */
@Component
class SnsProducerRouteBuilder(
    private val testSpringServiceProperties: TestSpringServiceProperties,
) : RouteBuilder() {

    override fun configure() {
        from("direct:sns-producer-route")
            .marshal().json(JsonLibrary.Jackson)
            .log("Sending message to AWS SNS topic..")
            .to("aws2-sns://${testSpringServiceProperties.aws.snsTopicName}")
    }
}
