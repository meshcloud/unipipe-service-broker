package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.ServiceBrokerFixture
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.revwalk.RevCommit
import org.junit.After
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.servicebroker.model.PlatformContext
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest
import java.io.File
import java.util.concurrent.Executors


class ConcurrentGitInteractionsTest {

  private lateinit var fixture: ServiceBrokerFixture

  @Before
  fun before() {
    fixture = ServiceBrokerFixture("src/test/resources/catalog.yml")
  }

  @After
  fun cleanUp() {
    fixture.close()
  }

  private fun makeSut(): GenericServiceInstanceService {
    return GenericServiceInstanceService(fixture.contextFactory, fixture.catalogService)
  }

  @Test
  fun `can process concurrent instance requests`() {
    val sut = makeSut()

    val ids = (0..9).map { "000000$it-7e05-4d5a-97b8-f8c5d1c710ab" }
    val requests = ids.map { createServiceInstanceRequest(it) }

    val executor = Executors.newFixedThreadPool(10)
    try {
      requests.forEach { executor.run { sut.createServiceInstance(it).block() } }
    } finally {
      executor.shutdown()
    }

    val log = fixture.gitHandler.getLog()
        .map { it.shortMessage }
        .toList()

    val expectedMessages = ids.map { "OSB API: Created Service instance $it" }
    assertEquals(expectedMessages, log.sorted()) // note: have to sort logs as the order is not guaranteed
  }
  

  private fun createServiceInstanceRequest(instanceId: String): CreateServiceInstanceRequest {
    return CreateServiceInstanceRequest
        .builder()
        .serviceDefinitionId("d40133dd-8373-4c25-8014-fde98f38a728")
        .planId("a13edcdf-eb54-44d3-8902-8f24d5acb07e")
        .serviceInstanceId(instanceId)
        .originatingIdentity(PlatformContext.builder().property("user", "unittester").build())
        .asyncAccepted(true)
        .serviceDefinition(fixture.catalogService.getCatalogInternal().serviceDefinitions.first())
        .build()
  }

}