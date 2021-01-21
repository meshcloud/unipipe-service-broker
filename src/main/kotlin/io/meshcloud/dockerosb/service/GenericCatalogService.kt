package io.meshcloud.dockerosb.service

import io.meshcloud.dockerosb.persistence.GitHandler
import io.meshcloud.dockerosb.persistence.YamlHandler
import mu.KotlinLogging
import org.springframework.cloud.servicebroker.model.catalog.Catalog
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition
import org.springframework.cloud.servicebroker.service.CatalogService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger { }

@Service
class GenericCatalogService(
    private val yamlHandler: YamlHandler,
    private val gitHandler: GitHandler
) : CatalogService {
    private var catalog: Catalog = parseCatalog(gitHandler, yamlHandler)

    @PostConstruct
    fun init(){
        gitHandler.pull()
        this.catalog = parseCatalog(gitHandler, yamlHandler)
    }

    private class YamlCatalog(
        val services: List<ServiceDefinition>
    )
    companion object {

        fun parseCatalog(gitHandler: GitHandler, yamlHandler: YamlHandler): Catalog {
            val statusYml = gitHandler.fileInRepo("catalog.yml")

            if (!statusYml.isFile) {
                log.error { "Could not read catalog.yml file from '${statusYml.absolutePath}'. Will start with an empty catalog." }
                return Catalog.builder().build()
            }

            val catalog = yamlHandler.readObject(statusYml,YamlCatalog::class.java)
            return Catalog.builder()
                .serviceDefinitions(catalog.services)
                .build()
        }
    }

    override fun getCatalog(): Mono<Catalog> {
        init()
        return  Mono.just(parseCatalog(gitHandler, yamlHandler))
    }
    /**
    * used to provide Catalog object to be used by other services internally
    */
    fun getCatalogInternal(): Catalog {
            return this.catalog
          }

    override fun getServiceDefinition(serviceId: String?): Mono<ServiceDefinition> {
        TODO("Not yet implemented")
    }
}