package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.ServiceBrokerFixture
import io.meshcloud.dockerosb.model.ServiceBinding
import io.meshcloud.dockerosb.model.Status
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest
import org.springframework.cloud.servicebroker.model.instance.OperationState
import java.io.File

internal class GenericServiceInstanceBindingServiceTest {

  private lateinit var fixture: ServiceBrokerFixture

  @Before
  fun before() {
    fixture = ServiceBrokerFixture("src/test/resources/catalog.yml")
  }

  @After
  fun cleanUp() {
    fixture.close()
  }

  private fun makeSut(): GenericServiceInstanceBindingService {
    return GenericServiceInstanceBindingService(fixture.contextFactory)
  }

  @Test
  fun `createServiceInstanceBinding creates binding yml file`() {
    val sut = makeSut()

    val request = fixture.builder.createServiceInstanceBindingRequest("instanceId", "bindingId")

    sut.createServiceInstanceBinding(request).block()

    val bindingYml = File("${fixture.localGitPath}/instances/${request.serviceInstanceId}/bindings/${request.bindingId}/binding.yml")
    Assert.assertTrue(bindingYml.exists())

    val msg = fixture.gitHandler.getLastCommitMessage()
    Assert.assertEquals("OSB API: Created Service binding bindingId", msg)
  }

  @Test
  fun `createServiceInstanceBinding without async accepted throws`() {
    val sut = makeSut()

    val request = fixture.builder.createServiceInstanceBindingRequest("instanceId", "bindingId") {
      asyncAccepted(false)
    }

    Assert.assertThrows(ServiceBrokerAsyncRequiredException::class.java) {
      sut.createServiceInstanceBinding(request).block()
    }
  }

  @Test
  fun `deleteServiceInstanceBinding without async accepted succeeds when binding does not exist`() {
    val sut = makeSut()

    val request = fixture.builder.deleteServiceInstanceBindingRequest("instanceId", "bindingId") {
      asyncAccepted(false)
    }

    sut.deleteServiceInstanceBinding(request).block()
  }

  @Test
  fun `deleteServiceInstanceBinding sets binding yml file status to deleted`() {
    val sut = makeSut()

    val createRequest = fixture.builder.createServiceInstanceBindingRequest("instanceId", "bindingId")
    sut.createServiceInstanceBinding(createRequest).block()

    val deleteRequest = fixture.builder.deleteServiceInstanceBindingRequest("instanceId", "bindingId")

    sut.deleteServiceInstanceBinding(deleteRequest).block()

    val bindingYml = File("${fixture.localGitPath}/instances/${createRequest.serviceInstanceId}/bindings/${createRequest.bindingId}/binding.yml")
    Assert.assertTrue(bindingYml.exists())

    val binding = fixture.yamlHandler.readObject(bindingYml, ServiceBinding::class.java)
    Assert.assertTrue(binding.deleted)

    val msg = fixture.gitHandler.getLastCommitMessage()
    Assert.assertEquals("OSB API: Marked Service binding bindingId as deleted.", msg)
  }

  @Test
  fun `getLastOperation returns IN_PROGRESS status when no status yaml exists`() {
    val sut = makeSut()

    val request = GetLastServiceBindingOperationRequest
        .builder()
        .serviceInstanceId("test-567")
        .serviceDefinitionId("my-def")
        .bindingId("bindingId")
        .build()

    val response = sut.getLastOperation(request).block()!!

    Assert.assertEquals(OperationState.IN_PROGRESS, response.state)
    Assert.assertEquals("preparing binding", response.description)
  }


  @Test
  fun `getLastOperation returns correct status from status yaml`() {
    val serviceInstanceId = "test-123"
    val bindingId = "bindingId"
    val statusYml = File("${fixture.localGitPath}/instances/${serviceInstanceId}/bindings/${bindingId}/status.yml")

    val status = Status(status = "succeeded", description = "binding successful")
    fixture.yamlHandler.writeObject(status, statusYml)

    val sut = makeSut()

    val request = GetLastServiceBindingOperationRequest
        .builder()
        .serviceInstanceId(serviceInstanceId)
        .serviceDefinitionId("my-def")
        .bindingId(bindingId)
        .build()

    val response = sut.getLastOperation(request).block()!!

    Assert.assertEquals(OperationState.SUCCEEDED, response.state)
    Assert.assertEquals("binding successful", response.description)
  }

  @Test
  fun `deleteServiceInstanceBinding sets binding status to preparing deletion`() {
    val sut = makeSut()

    val createRequest = fixture.builder.createServiceInstanceBindingRequest("instanceId", "bindingId")
    sut.createServiceInstanceBinding(createRequest).block()

    val deleteRequest = fixture.builder.deleteServiceInstanceBindingRequest("instanceId", "bindingId")

    sut.deleteServiceInstanceBinding(deleteRequest).block()

    val statusYml = File("${fixture.localGitPath}/instances/${createRequest.serviceInstanceId}/bindings/${createRequest.bindingId}/status.yml")
    Assert.assertTrue(statusYml.exists())

    val status = fixture.yamlHandler.readObject(statusYml, Status::class.java)
    Assert.assertEquals("in progress", status.status)
    Assert.assertEquals("preparing binding deletion", status.description)

  }
}