package io.meshcloud.dockerosb.metrics.gauge

import io.meshcloud.dockerosb.metrics.MetricsResponse
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
class PagedGaugeController(
    private val metricsProviders: List<PaginatedGaugeMetricsProvider>
) {

  @GetMapping("/metrics/gauges/{serviceDefinitionId}")
  fun getGaugeMetricValues(
      @PathVariable("serviceDefinitionId") serviceDefinitionId: String,
      @RequestParam from: Instant,
      @RequestParam to: Instant
  ): ResponseEntity<MetricsResponse<GaugeMetricModel>> {
    return getResponse(serviceDefinitionId, from, to, 0)
  }

  @GetMapping("/metrics/gauges/{serviceDefinitionId}/{index}")
  fun getGaugeMetricValues(
      @PathVariable("serviceDefinitionId") serviceDefinitionId: String,
      @PathVariable(name = "index", required = false) index: Int,
      @RequestParam from: Instant,
      @RequestParam to: Instant
  ): ResponseEntity<MetricsResponse<GaugeMetricModel>> {
    return getResponse(serviceDefinitionId, from, to, index)
  }

  private fun getResponse(serviceDefinitionId: String, from: Instant, to: Instant, instanceIndex: Int): ResponseEntity<MetricsResponse<GaugeMetricModel>> {
    val provider = metricsProviders.firstOrNull { it.canHandle(serviceDefinitionId) }
        ?: throw IllegalArgumentException("Could not find a matching provider for service id $serviceDefinitionId!")
    val dataPoints = provider.getMetrics(serviceDefinitionId, from, to, instanceIndex)
    val response = MetricsResponse(dataPoints)
    val hasMorePages = instanceIndex < provider.totalInstanceCount() - 1
    if (hasMorePages) {
      val nextLink = linkTo(methodOn(PagedGaugeController::class.java).getGaugeMetricValues(
          serviceDefinitionId, instanceIndex + 1, from, to))
      response.add(nextLink.withRel("next"))
    }
    return ResponseEntity(response, HttpStatus.OK)
  }
}
