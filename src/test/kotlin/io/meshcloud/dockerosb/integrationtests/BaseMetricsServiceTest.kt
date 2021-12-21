package io.meshcloud.dockerosb.integrationtests

import io.meshcloud.dockerosb.config.AppConfig
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.io.File


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseMetricsServiceTest {

  @LocalServerPort
  private var port: Int = 0

  @Autowired
  private lateinit var appConfig: AppConfig

  @Autowired
  private lateinit var restTemplate: TestRestTemplate

  /**
   * Do cleanup both before and after to ensure a clean status before and after tests
   */
  @Before
  @After
  fun cleanUp() {
    //FileUtils.deleteDirectory(File(appConfig.storagePath))
  }

  protected fun provisionServiceInstance(instanceId: String, serviceId: String, planId: String) {
    val response = restTemplateWithBasicAuth().exchange(
        baseUrl() + "v2/service_instances/$instanceId",
        HttpMethod.PUT,
        HttpEntity("""
          {
            "service_id": "$serviceId", 
            "plan_id": "$planId"
          }
          """,
            getOsbApiRequestHeaders()
        ),
        Map::class.java
    )
    Assert.assertEquals("Created service instance", response.body?.get("operation"))
  }

  private fun getOsbApiRequestHeaders(): HttpHeaders {
    return HttpHeaders().apply {
      set("X-Broker-API-Originating-Identity", "Test ewogICJ1c2VyX2lkIjogIjY4M2VhNzQ4LTMwOTItNGZmNC1iNjU2LTM5Y2FjYzRkNTM2MCIKfQ==")
      contentType = MediaType.APPLICATION_JSON
    }
  }

  fun baseUrl(): String {
    return "http://localhost:$port/"
  }

  fun restTemplateWithBasicAuth(): TestRestTemplate {
    return this.restTemplate.withBasicAuth(appConfig.basicAuthUsername, appConfig.basicAuthPassword)
  }
}
