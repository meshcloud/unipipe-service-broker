package io.meshcloud.dockerosb.persistence

import com.fasterxml.jackson.core.type.TypeReference
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import io.meshcloud.dockerosb.metrics.inplace.InplaceMetricModel
import io.meshcloud.dockerosb.model.ServiceInstance
import io.meshcloud.dockerosb.model.Status
import mu.KotlinLogging
import org.springframework.cloud.servicebroker.model.instance.OperationState
import org.springframework.stereotype.Component
import java.io.File

private val log = KotlinLogging.logger {}

@Component
class ServiceInstanceRepository(private val yamlHandler: YamlHandler, private val metricYamlHandler: MetricYamlHandler, private val gitHandler: GitHandler) {
  fun createServiceInstance(serviceInstance: ServiceInstance) {
    val serviceInstanceId = serviceInstance.serviceInstanceId

    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)

    yamlHandler.writeObject(
        objectToWrite = serviceInstance,
        file = instanceYml
    )
    gitHandler.commitAllChanges(
        commitMessage = "Created Service instance $serviceInstanceId"
    )
  }

  // TODO Check if an update request is allowed. See https://github.com/meshcloud/unipipe-service-broker/pull/35/files#r651527916
  //
  //  There are several actions that can be [triggered via an update request](https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#updating-a-service-instance):
  //- updating the plan a service instance is using, if `plan_updateable` is true
  //- updating the context object of a service instance, if `allow_context_updates` is true
  //- applying a maintenance update, if the service broker previously provided `maintenance_info` to the platform.
  fun updateServiceInstance(serviceInstance: ServiceInstance) {
    val serviceInstanceId = serviceInstance.serviceInstanceId

    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)

    yamlHandler.writeObject(
      objectToWrite = serviceInstance,
      file = instanceYml
    )
    gitHandler.commitAllChanges(
      commitMessage = "Updated Service instance $serviceInstanceId"
    )
  }

  fun deleteServiceInstance(serviceInstance: ServiceInstance) {
    val serviceInstanceId = serviceInstance.serviceInstanceId

    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)
    serviceInstance.deleted = true
    yamlHandler.writeObject(
        objectToWrite = serviceInstance,
        file = instanceYml
    )

    val statusYml = serviceInstanceStatusYmlFile(serviceInstanceId)
    val status = Status("in progress", "preparing service deletion")
    yamlHandler.writeObject(
        objectToWrite = status,
        file = statusYml
    )

    gitHandler.commitAllChanges(commitMessage = "Marked Service instance $serviceInstanceId as deleted.")
  }

  fun tryGetServiceInstance(serviceInstanceId: String): ServiceInstance? {
    val instanceYml = serviceInstanceYmlFile(serviceInstanceId)

    if (!instanceYml.exists()) {
      return null
    }

    return yamlHandler.readObject(instanceYml, ServiceInstance::class.java)
  }

  // TODO make metricType an enum
  // TODO consider introducing an common base type (interface) for all metric types,
  //      so we can get rid of ServiceInstanceDatapoints<*> and can use e.g. ServiceInstanceDatapoints<MetricModel>
  fun tryGetServiceInstanceMetrics(serviceInstanceId: String, metricType: String): ServiceInstanceDatapoints<*>? {
    val instanceMetricsYml = serviceInstanceYmlFile(serviceInstanceId)
    return when(metricType) {
      "gauge" -> metricYamlHandler.readGaugeServiceInstanceDatapoints(instanceMetricsYml)
      "inplace" -> metricYamlHandler.readInplaceServiceInstanceDatapoints(instanceMetricsYml)
      // ..
      else -> null
    }

  }


  /*fun tryGetServiceInstanceMetrics(serviceInstanceId: String): ServiceInstanceDatapoints<T>? {
    val instanceMetricsYml = serviceInstanceYmlFile(serviceInstanceId)

    if (!instanceMetricsYml.exists()) {
      return null
    }

    return yamlHandler.readObject(instanceMetricsYml, ServiceInstanceDatapoints::class.java)
  }*/

  fun findInstancesByServiceId(serviceDefinitionId: String): List<ServiceInstance> {
    return gitHandler.instancesDirectory().listFiles()
        .map { gitHandler.fileInRepo(gitHandler.instanceYmlRelativePath(it.name)) }
        .filter { it.exists() }
        .sortedBy { it.lastModified() }
        .map { yamlHandler.readObject(it, ServiceInstance::class.java) }
        .filter { it.serviceDefinitionId == serviceDefinitionId }
  }

  fun getServiceInstanceMetricsInplace(serviceInstanceId: String): ServiceInstanceDatapoints<InplaceMetricModel>? {
    val metricsYml = serviceInstanceMetricsYmlFile(serviceInstanceId)

    return if (metricsYml.exists())
    {
      when {}
       yamlHandler.readObject(metricsYml, ServiceInstanceDatapoints<InplaceMetricModel>::class.java)
    }
      else {
         null
      }
  }

  fun getServiceInstanceStatus(serviceInstanceId: String): Status {
    val statusYml = serviceInstanceStatusYmlFile(serviceInstanceId)

    return when (statusYml.exists()) {
      true -> yamlHandler.readObject(statusYml, Status::class.java)
      else -> Status(
          status = OperationState.IN_PROGRESS.value,
          description = "preparing deployment"
      )
    }
  }

  private fun serviceInstanceMetricsYmlFile(serviceInstanceId: String): File {
    val instanceMetricPath = instanceFolderPath(serviceInstanceId) + "/metrics.yml"

    return gitHandler.fileInRepo(instanceMetricPath)
  }

  private fun serviceInstanceYmlFile(serviceInstanceId: String): File {
    val instanceYmlPath = instanceFolderPath(serviceInstanceId) + "/instance.yml"

    return gitHandler.fileInRepo(instanceYmlPath)
  }

  private fun serviceInstanceStatusYmlFile(serviceInstanceId: String): File {
    val instanceYmlPath = instanceFolderPath(serviceInstanceId) + "/status.yml"

    return gitHandler.fileInRepo(instanceYmlPath)
  }

  private fun instanceFolderPath(serviceInstanceId: String): String {
    return "instances/$serviceInstanceId"
  }
}

