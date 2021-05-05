package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.ServiceBrokerFixture
import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.model.Status
import io.meshcloud.dockerosb.persistence.GitHandlerService
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException
import org.springframework.cloud.servicebroker.model.PlatformContext
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest
import org.springframework.cloud.servicebroker.model.instance.OperationState
import java.io.File

class GenericServiceInstanceServiceTest {

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
    return GenericServiceInstanceService(fixture.contextFactory)
  }

  @Test
  fun `createServiceInstance creates expected yaml`() {
    val sut = makeSut()

    val request = createServiceInstanceRequest()

    sut.createServiceInstance(request).block()

    val yamlPath = "${fixture.localGitPath}/instances/${request.serviceInstanceId}/instance.yml"
    val instanceYml = File(yamlPath)

    val expectedYamlPath = "src/test/resources/expected_instance.yml"
    val expectedInstanceYml = File(expectedYamlPath)

    assertTrue("instance.yml does not exist in $yamlPath", instanceYml.exists())
    assertTrue("expected_instance.yml does not exist in $expectedYamlPath", expectedInstanceYml.exists())

    assertTrue(FileUtils.contentEquals(expectedInstanceYml, instanceYml))
  }

  @Test
  fun `createServiceInstance creates git commit`() {
    val sut = makeSut()

    val request = createServiceInstanceRequest()

    sut.createServiceInstance(request).block()

    val gitHandler = GitHandlerService(fixture.gitConfig)

    assertTrue(gitHandler.getLastCommitMessage().contains(request.serviceInstanceId))
  }

  private fun createServiceInstanceRequest(): CreateServiceInstanceRequest {
    return CreateServiceInstanceRequest
        .builder()
        .serviceDefinitionId("d40133dd-8373-4c25-8014-fde98f38a728")
        .planId("a13edcdf-eb54-44d3-8902-8f24d5acb07e")
        .serviceInstanceId("e4bd6a78-7e05-4d5a-97b8-f8c5d1c710ab")
        .originatingIdentity(PlatformContext.builder().property("user", "unittester").build())
        .asyncAccepted(true)
        .serviceDefinition(fixture.catalogService.cachedServiceDefinitions().first())
        .build()
  }

  @Test
  fun `getLastOperation returns correct status from status yaml`() {
    val serviceInstanceId = "test-123"
    val statusYamlPath = "src/test/resources/status.yml"
    val statusYmlFile = File(statusYamlPath)
    val statusYmlDestinationDir = File("${fixture.gitConfig.localPath}/instances/$serviceInstanceId/")
    FileUtils.forceMkdir(statusYmlDestinationDir)
    FileUtils.copyFileToDirectory(statusYmlFile, statusYmlDestinationDir)

    val sut = makeSut()

    val request = GetLastServiceOperationRequest
        .builder()
        .serviceInstanceId(serviceInstanceId)
        .serviceDefinitionId("my-def")
        .build()

    val response = sut.getLastOperation(request).block()!!

    assertEquals(OperationState.SUCCEEDED, response.state)
    assertEquals("deployment successful", response.description)
  }

  @Test
  fun `getLastOperation returns IN_PROGRESS status when no status yaml exists`() {
    val sut = makeSut()

    val request = GetLastServiceOperationRequest
        .builder()
        .serviceInstanceId("test-567")
        .serviceDefinitionId("my-def")
        .build()

    val response = sut.getLastOperation(request).block()!!

    assertEquals(OperationState.IN_PROGRESS, response.state)
    assertEquals("preparing deployment", response.description)
  }


  @Test
  fun `deleting a service without asyncAccepted throws`() {
    val sut = makeSut()

    val serviceInstanceId = copyInstanceYmlToRepo()

    val request = DeleteServiceInstanceRequest
        .builder()
        .serviceInstanceId(serviceInstanceId)
        .serviceDefinitionId("my-def")
        .asyncAccepted(false)
        .build()

    assertThrows(ServiceBrokerAsyncRequiredException::class.java) {
      sut.deleteServiceInstance(request).block()!!
    }
  }

  @Test
  fun `instance yaml is correctly updated after delete Service Instance`() {
    val sut = makeSut()

    val serviceInstanceId = copyInstanceYmlToRepo()

    val request = DeleteServiceInstanceRequest
        .builder()
        .serviceInstanceId(serviceInstanceId)
        .serviceDefinitionId("my-def")
        .asyncAccepted(true)
        .build()


    val response = sut.deleteServiceInstance(request).block()!!

    assertEquals(true, response.isAsync)
    assertNotNull(response.operation)

    val updatedInstanceYml = File("${fixture.localGitPath}/instances/${request.serviceInstanceId}/instance.yml")
    val updatedInstance = fixture.yamlHandler.readObject(updatedInstanceYml, ServiceInstance::class.java)

    assertEquals(true, updatedInstance.deleted)
  }

  @Test
  fun `status is correctly updated after delete Service Instance`() {
    val sut = makeSut()

    val serviceInstanceId = copyInstanceYmlToRepo()

    val request = DeleteServiceInstanceRequest
        .builder()
        .serviceInstanceId(serviceInstanceId)
        .serviceDefinitionId("my-def")
        .asyncAccepted(true)
        .build()


    sut.deleteServiceInstance(request).block()

    val updatedStatusYml = File("${fixture.localGitPath}/instances/${request.serviceInstanceId}/status.yml")
    val updatedStatus = fixture.yamlHandler.readObject(updatedStatusYml, Status::class.java)

    assertEquals("in progress", updatedStatus.status)
    assertEquals("preparing service deletion", updatedStatus.description)
  }

  private fun copyInstanceYmlToRepo(): String {
    val serviceInstanceId = "e4bd6a78-7e05-4d5a-97b8-f8c5d1c710ab"
    val instanceYmlPath = "${fixture.localGitPath}/instances/$serviceInstanceId/instance.yml"

    val existingInstanceYml = File("src/test/resources/expected_instance.yml")
    val instanceYmlInRepo = File(instanceYmlPath)

    FileUtils.copyFile(existingInstanceYml, instanceYmlInRepo)

    return serviceInstanceId
  }
}