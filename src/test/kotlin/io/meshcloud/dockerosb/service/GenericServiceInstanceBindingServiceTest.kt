package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.ServiceBrokerFixture
import io.meshcloud.dockerosb.model.ServiceBinding
import io.meshcloud.dockerosb.persistence.YamlHandler
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException
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
}