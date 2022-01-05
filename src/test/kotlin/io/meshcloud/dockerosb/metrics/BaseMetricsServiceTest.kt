package io.meshcloud.dockerosb.metrics

import io.meshcloud.dockerosb.ServiceBrokerFixture
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import io.meshcloud.dockerosb.service.GenericCatalogService
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.springframework.test.context.ActiveProfiles
import java.io.File


@ActiveProfiles("test")
abstract class BaseMetricsServiceTest {

  private lateinit var fixture: ServiceBrokerFixture

  lateinit var serviceInstanceRepository: ServiceInstanceRepository

  lateinit var catalogService: GenericCatalogService

  @Before
  fun before() {
    fixture = ServiceBrokerFixture("src/test/resources/catalog.yml")
    FileUtils.copyDirectory(File("src/test/resources/instances"), File("${fixture.localGitPath}/instances"))
    catalogService = GenericCatalogService(fixture.contextFactory)
    serviceInstanceRepository = ServiceInstanceRepository(fixture.yamlHandler, fixture.gitHandler)
  }

  @After
  fun cleanUp() {
    fixture.close()
    FileUtils.deleteDirectory(File(fixture.localGitPath))
  }
}
