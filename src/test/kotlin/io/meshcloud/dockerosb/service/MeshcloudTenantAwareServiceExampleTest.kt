package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.ServiceBrokerFixture
import io.meshcloud.dockerosb.config.CatalogConfiguration
import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.model.Status
import io.meshcloud.dockerosb.persistence.GitHandler
import io.meshcloud.dockerosb.persistence.YamlHandler
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.servicebroker.model.PlatformContext
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest
import org.springframework.cloud.servicebroker.model.instance.OperationState
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * This test simulates interaction of a tenant-aware service broker that supports the meshcloud "platform tenant"
 * Binding Resource type.
 *
 * See https://docs.meshcloud.io/docs/meshstack.meshmarketplace.custom.html#tenant-aware-service-broker for more information
 *
 */
class MeshcloudTenantAwareServiceExampleTest {

  private lateinit var fixture: ServiceBrokerFixture

  @Before
  fun before() {
    val catalogFile = testFile("catalog.yml")
    fixture = ServiceBrokerFixture(catalogFile.path)
  }

  @After
  fun cleanUp() {
    fixture.close()
  }

  private fun makeSut(): GenericServiceInstanceService {
    return GenericServiceInstanceService(fixture.yamlHandler, fixture.gitHandler, fixture.catalog)
  }

  @Test
  fun `createServiceInstance creates expected yaml`() {
    val sut = makeSut()

    val request = createServiceInstanceRequest()

    sut.createServiceInstance(request).block()

    val yamlPath = "${fixture.localGitPath}/instances/${request.serviceInstanceId}/instance.yml"
    val instanceYml = File(yamlPath)

    val expectedInstanceYml = testFile("expected_instance.yml")

    assertTrue("instance.yml does not exist in $yamlPath", instanceYml.exists())
    assertTrue("expected_instance.yml does not exist in ${expectedInstanceYml.path}", expectedInstanceYml.exists())

    verifyFilesEqual(expectedInstanceYml, instanceYml)
  }

  private fun testFile(filename: String): File {
    val basePath = "src/test/resources/tenant-aware"
    val path = Paths.get(basePath, filename)

    return path.toFile()
  }

  private fun createServiceInstanceRequest(): CreateServiceInstanceRequest {
    val catalog = CatalogConfiguration(YamlHandler(), GitHandler(fixture.gitConfig)).catalog()

    return CreateServiceInstanceRequest
        .builder()
        .serviceDefinitionId("d40133dd-8373-4c25-8014-fde98f38a728")
        .planId("a13edcdf-eb54-44d3-8902-8f24d5acb07e")
        .serviceInstanceId("e4bd6a78-7e05-4d5a-97b8-f8c5d1c710ab")
        .originatingIdentity(PlatformContext.builder().property("user", "unittester").build())
        .asyncAccepted(true)
        .serviceDefinition(catalog.serviceDefinitions.first())
        .build()
  }

  private fun verifyFilesEqual(expectedInstanceYml: File, instanceYml: File) {
    val expected = FileUtils.readFileToString(expectedInstanceYml)
    val actual = FileUtils.readFileToString(instanceYml)
    assertEquals(expected, actual)
  }
}