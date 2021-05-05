package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.ServiceBrokerFixture
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.servicebroker.model.PlatformContext
import org.springframework.cloud.servicebroker.model.binding.BindResource
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest
import java.io.File
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

  @Test
  fun `createServiceInstance creates expected yaml`() {
    val sut = GenericServiceInstanceService(fixture.contextFactory)

    val request = CreateServiceInstanceRequest
        .builder()
        .serviceDefinitionId("d40133dd-8373-4c25-8014-fde98f38a728")
        .planId("a13edcdf-eb54-44d3-8902-8f24d5acb07e")
        .parameters(
            "securityContact", "soc@example.com"
        )
        .serviceInstanceId("e4bd6a78-7e05-4d5a-97b8-f8c5d1c710ab")
        .originatingIdentity(PlatformContext.builder().property("user", "unittester").build())
        .asyncAccepted(true)
        .serviceDefinition(fixture.catalogService.cachedServiceDefinitions().first())
        .build()

    sut.createServiceInstance(request).block()

    val instanceYml = File("${fixture.localGitPath}/instances/${request.serviceInstanceId}/instance.yml")
    val expectedInstanceYml = testFile("expected_instance.yml")

    verifyFilesEqual(expectedInstanceYml, instanceYml)
  }

  @Test
  fun `createServiceInstanceBinding creates expected yaml`() {
    val sut = GenericServiceInstanceBindingService(fixture.contextFactory)

    val properties = mapOf(
        "tenant_id" to "subscriptionid-123",
        "platform" to "meshLocation.meshPlatform"
    )

    val bindResource = BindResource.builder()
        .properties(properties)
        .build()

    val request = CreateServiceInstanceBindingRequest.builder()
        .serviceDefinitionId("d40133dd-8373-4c25-8014-fde98f38a728")
        .planId("a13edcdf-eb54-44d3-8902-8f24d5acb07e")
        .serviceInstanceId("e4bd6a78-7e05-4d5a-97b8-f8c5d1c710ab")
        .originatingIdentity(PlatformContext.builder().property("user", "unittester").build())
        .asyncAccepted(true)
        .serviceDefinition(fixture.catalogService.cachedServiceDefinitions().first())
        .bindingId("77643a12-a1d1-4717-abcd-9d66448a2148")
        .bindResource(bindResource)
        .build()

    sut.createServiceInstanceBinding(request)

    val bindingYml = File("${fixture.localGitPath}/instances/${request.serviceInstanceId}/bindings/${request.bindingId}/binding.yml")
    val expectedYml = testFile("expected_binding.yml")

    verifyFilesEqual(expectedYml, bindingYml)
  }

  private fun testFile(filename: String): File {
    val basePath = "src/test/resources/tenant-aware"
    val path = Paths.get(basePath, filename)

    return path.toFile()
  }

  private fun verifyFilesEqual(expected: File, actual: File) {
    assertTrue("expected file does not exist in ${expected.path}", expected.exists())
    assertTrue("actual file does not exist in ${actual.path}", actual.exists())

    val expectedContent = FileUtils.readFileToString(expected)
    val actualContent = FileUtils.readFileToString(actual)

    assertEquals(expectedContent, actualContent)
  }
}