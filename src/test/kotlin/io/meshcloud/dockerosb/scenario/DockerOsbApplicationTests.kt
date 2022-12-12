package io.meshcloud.dockerosb.scenario

import io.meshcloud.dockerosb.ServiceBrokerFixture
import io.meshcloud.dockerosb.persistence.GitOperationContextFactory
import io.meshcloud.dockerosb.persistence.ServiceInstanceRepository
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test")
abstract class DockerOsbApplicationTests {

  protected lateinit var fixture: ServiceBrokerFixture

  @Autowired
  private lateinit var gitContextFactory: GitOperationContextFactory

  @Before
  fun before() {
    fixture = ServiceBrokerFixture("src/test/resources/catalog.yml")
  }

  @After
  fun cleanUp() {
    fixture.close()
  }

  protected fun useServiceInstanceRepository(fn: (repository: ServiceInstanceRepository) -> Unit) {
    gitContextFactory.acquireContext().use {
      fn(it.buildServiceInstanceRepository())
    }
  }
}
