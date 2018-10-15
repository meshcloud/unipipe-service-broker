package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.config.CatalogConfiguration
import io.meshcloud.dockerosb.config.GitConfig
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

const val localGitPath = "tmp/test/git"

class GenericServiceInstanceServiceTest {

  private val config = GitConfig(
      localPath = localGitPath,
      remote = null,
      sshKey = null,
      username = null,
      password = null
  )

  @Before
  fun setUp() {
    FileUtils.copyFile(File("src/test/resources/catalog.yml"), File("$localGitPath/catalog.yml"))
  }

  @After
  fun cleanUp() {
    FileUtils.deleteDirectory(File(localGitPath))
  }

  @Test
  fun `createServiceInstance creates expected yaml`() {
    val sut = GenericServiceInstanceService(YamlHandler(), GitHandler(config))
    val request = createServiceInstanceRequest()

    sut.createServiceInstance(request)
    val yamlPath = "$localGitPath/instances/${request.serviceInstanceId}/instance.yml"
    val instanceYml = File(yamlPath)

    val expectedYamlPath = "src/test/resources/expected_instance.yml"
    val expectedInstanceYml = File(expectedYamlPath)

    assertTrue("instance.yml does not exist in $yamlPath", instanceYml.exists())
    assertTrue("expected_instance.yml does not exist in $expectedYamlPath", expectedInstanceYml.exists())

    assertTrue(FileUtils.contentEquals(expectedInstanceYml, instanceYml))
  }

  @Test
  fun `createServiceInstance creates git commit`() {
    val sut = GenericServiceInstanceService(YamlHandler(), GitHandler(config))
    val request = createServiceInstanceRequest()

    sut.createServiceInstance(request)

    val gitHandler = GitHandler(config)

    assertTrue(gitHandler.getLastCommitMessage().contains(request.serviceInstanceId))
  }

  private fun createServiceInstanceRequest(): CreateServiceInstanceRequest {
    val catalog = CatalogConfiguration(YamlHandler(), GitHandler(config)).catalog()
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

  @Test
  fun `getLastOperation returns correct status from status yaml`() {
    val serviceInstanceId = "test-123"
    val statusYamlPath = "src/test/resources/status.yml"
    val statusYmlFile = File(statusYamlPath)
    val statusYmlDestinationDir = File("${config.localPath}/instances/$serviceInstanceId/")
    FileUtils.forceMkdir(statusYmlDestinationDir)
    FileUtils.copyFileToDirectory(statusYmlFile, statusYmlDestinationDir)

    val sut = GenericServiceInstanceService(YamlHandler(), GitHandler(config))
    val request = GetLastServiceOperationRequest
        .builder()
        .serviceInstanceId(serviceInstanceId)
        .build()

    val response = sut.getLastOperation(request)

    assertEquals(OperationState.SUCCEEDED, response.state)
    assertEquals("deployment successful", response.description)
  }

  @Test
  fun `getLastOperation returns IN_PROGRESS status when no status yaml exists`() {
    val sut = GenericServiceInstanceService(YamlHandler(), GitHandler(config))
    val request = GetLastServiceOperationRequest
        .builder()
        .serviceInstanceId("test-567")
        .build()

    val response = sut.getLastOperation(request)

    assertEquals(OperationState.IN_PROGRESS, response.state)
    assertEquals("preparing deployment", response.description)
  }

  @Test
  fun `instance yaml is correctly updated after delete Service Instance`() {
    val yamlHandler = YamlHandler()
    val sut = GenericServiceInstanceService(yamlHandler, GitHandler(config))
    val request = DeleteServiceInstanceRequest
        .builder()
        .serviceInstanceId("test-567")
        .build()

    copyInstanceYmlToRepo(request.serviceInstanceId)

    val response = sut.deleteServiceInstance(request)

    assertEquals(true, response.isAsync)
    assertNotNull(response.operation)

    val updatedInstanceYml = File("$localGitPath/instances/${request.serviceInstanceId}/instance.yml")
    val updatedInstance = yamlHandler.readObject(updatedInstanceYml, ServiceInstance::class.java)

    assertEquals(true, updatedInstance.deleted)
  }

  @Test
  fun `status is correctly updated after delete Service Instance`() {
    val yamlHandler = YamlHandler()
    val sut = GenericServiceInstanceService(yamlHandler, GitHandler(config))
    val request = DeleteServiceInstanceRequest
        .builder()
        .serviceInstanceId("test-567")
        .build()

    copyInstanceYmlToRepo(request.serviceInstanceId)

    sut.deleteServiceInstance(request)

    val updatedStatusYml = File("$localGitPath/instances/${request.serviceInstanceId}/status.yml")
    val updatedStatus = yamlHandler.readObject(updatedStatusYml, Status::class.java)

    assertEquals("in progress", updatedStatus.status)
    assertEquals("preparing service deletion", updatedStatus.description)
  }

  private fun copyInstanceYmlToRepo(serviceInstanceId: String) {
    val instanceYmlPath = "$localGitPath/instances/$serviceInstanceId/instance.yml"

    val existingInstanceYml = File("src/test/resources/expected_instance.yml")
    val instanceYmlInRepo = File(instanceYmlPath)
    FileUtils.copyFile(existingInstanceYml, instanceYmlInRepo)
  }
}