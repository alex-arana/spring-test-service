# Testcontainers / Issue #7454

## Overview

This repository contains the source for building and running the test used to demonstrate testcontainers [issue 7454](https://github.com/testcontainers/testcontainers-java/discussions/7454).
Said issue was later [moved to a discussion](https://github.com/testcontainers/testcontainers-java/discussions/7454#discussioncomment-6879569) due presumably to a lack of clarity in trying
to identify the underlying cause(s).

[Issue #7454](https://github.com/testcontainers/testcontainers-java/discussions/7454) describes an error that floods the logs when [Ryuk](https://github.com/testcontainers/moby-ryuk) cleans
up test containers too eagerly as a consequence of a separate [JVM ShutdownHook](https://docs.oracle.com/javase/8/docs/technotes/guides/lang/hook-design.html) which was introduced with
testcontainers [#7717](https://github.com/testcontainers/testcontainers-java/commit/39f02190c01702fb3121b080f29c72bbdf7c78ea) back in October 2023. 

The following [discussion post](https://github.com/testcontainers/testcontainers-java/discussions/7454#discussioncomment-8111886) contains a more detailed description of the problem:

> I think the problem basically boils down to this:
> > - TestContainers uses Ryuk to handle cleaning up containers
> > - TestContainers registers a JVM shutdown hook to terminate Ryuk, which will in turn terminate and clean up any containers
> > - Spring leaves Application Contexts running until the JVM shuts down - the Listeners will also continue to run until the JVM shuts down 
> > - Spring, therefore, also registers a JVM shutdown hook 
> > - JVM shutdown hooks are not guaranteed to run in any particular order 
> > - JVM shutdown hooks run in parallel 
> > - Chances are good that it takes longer for Spring to get around to shutting down the listener than it does for Ryuk to terminate LocalStack.


## Requirements
In order to build and run the test, the following is required:

- Java 21 JDK (I used [Amazon Corretto](https://aws.amazon.com/corretto/) **21.0.2-amzn**). To install the said JDK using [SDKMAN](https://sdkman.io/) first clone this project and 
  then run this in a console:
  ```shell
  $ sdk env install
  ```
- A working Internet connection (to download all library dependencies required to build the project using [Gradle](https://gradle.org/)). The build script requires access to Maven Central
  and the Gradle Plugins repository.


## Running the tests

To run the test using [testcontainers 1.9.7](https://github.com/testcontainers/testcontainers-java/releases/tag/1.19.7), simply execute the following command at a terminal prompt:
```shell
$ ./gradlew clean test
```

The console output should display quite a few stacktraces at `WARN` level, such as the one below, before eventually shutting down. In the example below,
Camel took 9ms to perform its shutdown routine, which means it never really stood a chance to complete before _Ryuk_, which had at that point killed the
_localstack_ container and with it, the SQS service it subscribes to.

```shell
2024-04-12T08:34:22.452+10:00  WARN 4349 --- [test-spring-service] [qs://demo-queue] o.a.c.component.aws2.sqs.Sqs2Consumer    : Failed polling endpoint: aws2-sqs://demo-queue?amazonSQSClient=%23sqsClient&deleteIfFiltered=false. Will try again at next poll. Caused by: [software.amazon.awssdk.core.exception.SdkClientException - Unable to execute HTTP request: Connect to 127.0.0.1:63388 [/127.0.0.1] failed: Connection refused]

software.amazon.awssdk.core.exception.SdkClientException: Unable to execute HTTP request: Connect to 127.0.0.1:63388 [/127.0.0.1] failed: Connection refused
        at software.amazon.awssdk.core.exception.SdkClientException$BuilderImpl.build(SdkClientException.java:111) ~[sdk-core-2.25.21.jar:na]
        at software.amazon.awssdk.core.exception.SdkClientException.create(SdkClientException.java:47) ~[sdk-core-2.25.21.jar:na]
        at software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper.setLastException(RetryableStageHelper.java:223) ~[sdk-core-2.25.21.jar:na]
        at software.amazon.awssdk.core.internal.http.pipeline.stages.RetryableStage.execute(RetryableStage.java:83) ~[sdk-core-2.25.21.jar:na]
        at software.amazon.awssdk.core.internal.http.pipeline.stages.RetryableStage.execute(RetryableStage.java:36) ~[sdk-core-2.25.21.jar:na]
        at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206) ~[sdk-core-2.25.21.jar:na]
        at software.amazon.awssdk.core.internal.http.StreamManagingStage.execute(StreamManagingStage.java:56) ~[sdk-core-2.25.21.jar:na]
        at software.amazon.awssdk.core.internal.http.StreamManagingStage.execute(StreamManagingStage.java:36) ~[sdk-core-2.25.21.jar:na]
...
2024-04-12T08:34:22.678+10:00  INFO 4349 --- [test-spring-service] [tomcat-shutdown] o.s.b.w.e.tomcat.GracefulShutdown        : Graceful shutdown complete
2024-04-12T08:34:22.687+10:00  INFO 4349 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   : Apache Camel 4.5.0 (camel-1) is shutting down (timeout:1s0ms)
2024-04-12T08:34:22.694+10:00  INFO 4349 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   : Routes stopped (total:2)
2024-04-12T08:34:22.694+10:00  INFO 4349 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   :     Stopped route2 (aws2-sqs://demo-queue)
2024-04-12T08:34:22.694+10:00  INFO 4349 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   :     Stopped route1 (direct://sns-producer-route)
2024-04-12T08:34:22.696+10:00  INFO 4349 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   : Apache Camel 4.5.0 (camel-1) shutdown in 9ms (uptime:3s)
BUILD SUCCESSFUL in 14s
```

### Test without Ryuk JVM ShutdownHook

I've tested modifying testcontainers [#7717](https://github.com/testcontainers/testcontainers-java/commit/39f02190c01702fb3121b080f29c72bbdf7c78ea) to make the _Ryuk_ container JVM ShutdownHook optional based on the
value of a new configuration flag. To test those changes locally, you can install a custom version of _testcontainers_ to the local Maven repository (`~/.m2/repository`) by performing the following steps:
1. Clone fork from GitHub:
   ```shell
   $ git clone git@github.com:alex-arana/testcontainers-java.git
   ```
2. Build and Install to local Maven repository:
   ```shell
   $ ./gradlew publishMavenJavaPublicationToMavenLocal -Ptestcontainers.version=1.19.7-7454
   ```
3. Switch to the local copy of this repository and rerun test using newly published _testcontainers_ artifact in local Maven repository:
   ```shell
   # first, switch to local copy of this repository, then run:
   $ ./gradlew clean test -PtestcontainersVersion=1.19.7-7454
   ```

Inspecting the build output it is easy to see that all exception stacktraces are now gone and _testcontainers_ are not terminated until all SQS listeners have shutdown. Having the ability to bypass Ryuk's ShutdownHook
caters for the use of frameworks such as Spring Boot where the lifecycle of application components, or beans, is automatically managed. I also ran the test using [testcontainers 1.19.1](https://github.com/testcontainers/testcontainers-java/releases/tag/1.19.1)
with equally successful results.

```shell
> Task :test

TestSpringServiceApplicationTests > Publishing message to an SNS topic uploads a file to a target S3 bucket() PASSED

localstack/localstack | 2024-04-12T05:26:24.809  INFO --- [   asgi_gw_0] localstack.request.aws     : AWS sqs.ReceiveMessage => 200
localstack/localstack | 2024-04-12T05:26:25.331  INFO --- [   asgi_gw_0] localstack.request.aws     : AWS sqs.ReceiveMessage => 200
2024-04-12T15:26:25.467+10:00  INFO 25490 --- [test-spring-service] [tomcat-shutdown] o.s.b.w.e.tomcat.GracefulShutdown        : Graceful shutdown complete
2024-04-12T15:26:25.471+10:00  INFO 25490 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   : Apache Camel 4.5.0 (camel-1) is shutting down (timeout:1s0ms)
2024-04-12T15:26:25.477+10:00  INFO 25490 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   : Routes stopped (total:2)
2024-04-12T15:26:25.477+10:00  INFO 25490 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   :     Stopped route2 (aws2-sqs://demo-queue)
2024-04-12T15:26:25.477+10:00  INFO 25490 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   :     Stopped route1 (direct://sns-producer-route)
2024-04-12T15:26:25.480+10:00  INFO 25490 --- [test-spring-service] [ionShutdownHook] o.a.c.impl.engine.AbstractCamelContext   : Apache Camel 4.5.0 (camel-1) shutdown in 8ms (uptime:2s)
2024-04-12T15:26:25.484+10:00 DEBUG 25490 --- [test-spring-service] [ionShutdownHook] o.t.s.c.g.d.core.command.AbstrDockerCmd  : Cmd: e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e,false
2024-04-12T15:26:25.485+10:00 DEBUG 25490 --- [test-spring-service] [ionShutdownHook] o.t.s.c.g.d.c.e.InspectContainerCmdExec  : GET: DefaultWebTarget{path=[/containers/e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e/json], queryParams={}}
2024-04-12T15:26:25.497+10:00 TRACE 25490 --- [test-spring-service] [ionShutdownHook] o.testcontainers.utility.ResourceReaper  : Stopping container: e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e
2024-04-12T15:26:25.497+10:00 DEBUG 25490 --- [test-spring-service] [ionShutdownHook] o.t.s.c.g.d.core.command.AbstrDockerCmd  : Cmd: e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e,<null>
2024-04-12T15:26:25.498+10:00 TRACE 25490 --- [test-spring-service] [ionShutdownHook] o.t.s.c.g.d.c.exec.KillContainerCmdExec  : POST: DefaultWebTarget{path=[/containers/e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e/kill], queryParams={}}
2024-04-12T15:26:25.636+10:00 TRACE 25490 --- [test-spring-service] [ionShutdownHook] o.testcontainers.utility.ResourceReaper  : Stopped container: localstack/localstack:3.2.0
2024-04-12T15:26:25.636+10:00 DEBUG 25490 --- [test-spring-service] [ionShutdownHook] o.t.s.c.g.d.core.command.AbstrDockerCmd  : Cmd: e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e,false
2024-04-12T15:26:25.636+10:00 DEBUG 25490 --- [test-spring-service] [ionShutdownHook] o.t.s.c.g.d.c.e.InspectContainerCmdExec  : GET: DefaultWebTarget{path=[/containers/e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e/json], queryParams={}}
2024-04-12T15:26:25.648+10:00 TRACE 25490 --- [test-spring-service] [ionShutdownHook] o.testcontainers.utility.ResourceReaper  : Removing container: e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e
2024-04-12T15:26:25.648+10:00 DEBUG 25490 --- [test-spring-service] [ionShutdownHook] o.t.s.c.g.d.core.command.AbstrDockerCmd  : Cmd: e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e,true,true
2024-04-12T15:26:25.648+10:00 TRACE 25490 --- [test-spring-service] [ionShutdownHook] o.t.s.c.g.d.c.e.RemoveContainerCmdExec   : DELETE: DefaultWebTarget{path=[/containers/e41da28c6d6cc5ded8a10d2ca344233068e1bc77ef4d3cda5c3842c5aed06b7e], queryParams={v=[true], force=[true]}}
2024-04-12T15:26:25.660+10:00 DEBUG 25490 --- [test-spring-service] [ionShutdownHook] o.testcontainers.utility.ResourceReaper  : Removed container and associated volume(s): localstack/localstack:3.2.0
BUILD SUCCESSFUL in 12s
```

## Links

- [testcontainers-java fork for #7454](https://github.com/alex-arana/testcontainers-java)
- [Introduction to the Spring IoC Container and Beans](https://docs.spring.io/spring-framework/reference/core/beans/introduction.html)
- [Design of the Shutdown Hooks API](https://docs.oracle.com/javase/8/docs/technotes/guides/lang/hook-design.html)
