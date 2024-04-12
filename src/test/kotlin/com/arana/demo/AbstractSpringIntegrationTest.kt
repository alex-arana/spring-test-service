package com.arana.demo

import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractSpringIntegrationTest {
    @Autowired
    private lateinit var s3Client: S3Client

    @Autowired
    private lateinit var snsClient: SnsClient

    @Autowired
    private lateinit var sqsClient: SqsClient

    @Autowired
    protected lateinit var testSpringServiceProperties: TestSpringServiceProperties

    private val topicSubscriptionARNs = mutableListOf<String>()

    @AfterEach
    fun tearDown() {
        removeTopicSubscribers()
    }

    protected fun addTopicSubscriber(queueName: String) {
        val topicArn = snsClient.createTopic { it.name(testSpringServiceProperties.aws.snsTopicName) }.topicArn()
        val queueUrl = sqsClient.createQueue { it.queueName(queueName) }.queueUrl()
        val queueArn = getQueueArn(queueUrl)
        snsClient
            .subscribe { builder ->
                builder.endpoint(queueArn)
                    .protocol("sqs")
                    .returnSubscriptionArn(true)
                    .topicArn(topicArn)
                    .attributes(mapOf("RawMessageDelivery" to "true"))
            }
            .also { response ->
                topicSubscriptionARNs.add(response.subscriptionArn())
            }
    }

    protected fun readS3Object(bucketName: String): String {
        return buildString {
            val request = ListObjectsV2Request.builder().bucket(bucketName).build()
            val s3Objects = s3Client.listObjectsV2(request)
            when {
                s3Objects.hasContents() -> {
                    val s3Object = s3Objects.contents().first()
                    val getObjectRequest = GetObjectRequest.builder()
                        .bucket(testSpringServiceProperties.aws.s3BucketName)
                        .key(s3Object.key())
                        .build()
                    s3Client.getObject(getObjectRequest).reader().use { reader ->
                        append(reader.readText())
                    }
                }
            }
        }
    }

    private fun getQueueArn(queueUrl: String): String {
        val attributes = sqsClient
            .getQueueAttributes { it.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN) }
            .attributes()
        return checkNotNull(attributes[QueueAttributeName.QUEUE_ARN]) {
            "Unable to get ARN for SQS queue: $queueUrl"
        }
    }

    private fun removeTopicSubscribers() {
        topicSubscriptionARNs.forEach { arn ->
            snsClient.unsubscribe { it.subscriptionArn(arn) }
        }
    }
}
