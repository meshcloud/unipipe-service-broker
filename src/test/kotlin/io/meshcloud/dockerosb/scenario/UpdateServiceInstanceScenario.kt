package io.meshcloud.dockerosb.scenario

import io.meshcloud.dockerosb.service.GenericServiceInstanceService
import org.junit.Assert.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class UpdateServiceInstanceScenario : DockerOsbApplicationTests() {

  @Autowired
  private lateinit var serviceInstanceService: GenericServiceInstanceService

  @Test
  fun `update request with null plan id does update the service instance without the plan`() {
    val instanceId = "e4bd6a78-7e05-4d5a-97b8-f8c5d1c710da"
    val createRequest = fixture.builder.createServiceInstanceRequest(instanceId)
    serviceInstanceService.createServiceInstance(createRequest)

    val createdPlanId = useServiceInstanceRepository { repository ->
      val instance = repository.tryGetServiceInstance(instanceId)
      instance!!.planId
    }

    val updateRequestWithNullPlanId = fixture.builder.updateServiceInstanceRequest(instanceId) {
      planId(null)
    }

    val response = serviceInstanceService.updateServiceInstance(updateRequestWithNullPlanId)

    response.subscribe {
      assertEquals("updating service", it.operation)
    }

    val updatedPlanId = useServiceInstanceRepository { repository ->
      val instance = repository.tryGetServiceInstance(instanceId)
      instance!!.planId
    }

    assertEquals(createdPlanId, updatedPlanId)
  }

  @Test
  fun `update request sets status to in progress`() {
    val instanceId = "e4bd6a78-7e05-4d5a-97b8-f8c5d1c710db"
    val createRequest = fixture.builder.createServiceInstanceRequest(instanceId)
    serviceInstanceService.createServiceInstance(createRequest)

    val updateRequest = fixture.builder.updateServiceInstanceRequest(instanceId) {
      parameters("foo", "bar")
    }

    val response = serviceInstanceService.updateServiceInstance(updateRequest)

    response.subscribe {
      assertEquals("updating service", it.operation)
    }

    useServiceInstanceRepository { repository ->
      val instance = repository.getServiceInstanceStatus(instanceId)
      assertEquals("in progress", instance.status)
      assertEquals("updating service", instance.description)
    }
  }
}