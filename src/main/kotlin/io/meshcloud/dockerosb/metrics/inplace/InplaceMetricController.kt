package io.meshcloud.dockerosb.metrics.inplace

import io.meshcloud.dockerosb.metrics.MetricsResponse
import io.meshcloud.dockerosb.metrics.ServiceInstanceDatapoints
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant


@RestController
class PagedInplaceMetricController(
    private val metricsProviders: List<PaginatedInplaceMetricsProvider>
) {

  @GetMapping("/metrics/inplace/{serviceDefinitionId}")
  fun getInplaceMetricValues(
      @PathVariable("serviceDefinitionId") serviceDefinitionId: String,
      @RequestParam from: Instant,
      @RequestParam to: Instant
  ): ResponseEntity<MetricsResponse<InplaceMetricModel>> {
    return getResponse(serviceDefinitionId, from, to, 0)
  }

  @GetMapping("/metrics/inplace/{serviceDefinitionId}/{index}")
  fun getInplaceMetricValues(
      @PathVariable("serviceDefinitionId") serviceDefinitionId: String,
      @PathVariable(name = "index", required = false) index: Int,
      @RequestParam from: Instant,
      @RequestParam to: Instant
  ): ResponseEntity<MetricsResponse<InplaceMetricModel>> {
    return getResponse(serviceDefinitionId, from, to, index)
  }

  private fun getResponse(serviceDefinitionId: String, from: Instant, to: Instant, instanceIndex: Int): ResponseEntity<MetricsResponse<InplaceMetricModel>> {
    val provider = metricsProviders.firstOrNull { it.canHandle(serviceDefinitionId) }
        ?: throw IllegalArgumentException("Could not find a matching provider for service id $serviceDefinitionId!")
    val dataPoints = provider.getMetrics(serviceDefinitionId, from, to, instanceIndex)
    val response = MetricsResponse(dataPoints)
    val hasMorePages = instanceIndex < provider.totalInstanceCount() - 1
    if (hasMorePages) {
      val nextLink = linkTo(methodOn(PagedInplaceMetricController::class.java).getInplaceMetricValues(
          serviceDefinitionId, instanceIndex + 1, from, to))
      response.add(nextLink.withRel("next"))
    }
    return ResponseEntity(response, HttpStatus.OK)
  }
}
