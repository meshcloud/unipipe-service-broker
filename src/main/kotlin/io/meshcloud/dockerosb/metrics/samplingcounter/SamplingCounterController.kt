package io.meshcloud.dockerosb.metrics.samplingcounter

import io.meshcloud.dockerosb.metrics.MetricsResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class SamplingCounterController(
    private val metricsProviders: List<SamplingCounterMetricsProvider>
) {

  @GetMapping("/metrics/samplingCounters/{serviceDefinitionId}")
  fun getSamplingCounterMetrics(
      @PathVariable("serviceDefinitionId") serviceDefinitionId: String,
      @RequestParam from: Instant,
      @RequestParam to: Instant
  ): ResponseEntity<MetricsResponse<SamplingCounterMetricModel>> {
    val dataPoints = metricsProviders.first { it.canHandle(serviceDefinitionId) }.getMetrics(from, to)
    val response = MetricsResponse(dataPoints)
    return ResponseEntity(response, HttpStatus.OK)
  }

}
