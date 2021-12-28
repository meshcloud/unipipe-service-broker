package io.meshcloud.dockerosb.metrics.periodiccounter

import io.meshcloud.dockerosb.metrics.MetricsResponse
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant


@RestController
class PeriodicCounterController(
    private val metricsProviders: List<PeriodicCounterMetricProviderInterface>
) {

  @GetMapping("/metrics/periodicCounters/{serviceDefinitionId}")
  fun getPeriodicCounterMetricValues(
      @PathVariable("serviceDefinitionId") serviceDefinitionId: String,
      @RequestParam from: Instant,
      @RequestParam to: Instant
  ): ResponseEntity<MetricsResponse<PeriodicCounterMetricModel>> {
    return getResponse(serviceDefinitionId, from, to, 0)
  }

  @GetMapping("/metrics/periodicCounters/{serviceDefinitionId}/{index}")
  fun getPeriodicCounterMetricValues(
      @PathVariable("serviceDefinitionId") serviceDefinitionId: String,
      @PathVariable(name = "index", required = false) index: Int,
      @RequestParam from: Instant,
      @RequestParam to: Instant
  ): ResponseEntity<MetricsResponse<PeriodicCounterMetricModel>> {
    return getResponse(serviceDefinitionId, from, to, index)
  }

  private fun getResponse(serviceDefinitionId: String, from: Instant, to: Instant, instanceIndex: Int): ResponseEntity<MetricsResponse<PeriodicCounterMetricModel>> {
    val provider = metricsProviders.firstOrNull { it.canHandle(serviceDefinitionId) }
        ?: throw IllegalArgumentException("Could not find a matching provider for service id $serviceDefinitionId!")
    val dataPoints = provider.getMetrics(serviceDefinitionId, from, to, instanceIndex)
    val response = MetricsResponse(dataPoints)
    val hasMorePages = instanceIndex < provider.totalInstanceCount(serviceDefinitionId) - 1
    if (hasMorePages) {
      val nextLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(PeriodicCounterController::class.java).getPeriodicCounterMetricValues(
          serviceDefinitionId, instanceIndex + 1, from, to))
      response.add(nextLink.withRel("next"))
    }
    return ResponseEntity(response, HttpStatus.OK)
  }

}
