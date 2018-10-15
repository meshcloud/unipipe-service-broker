package io.meshcloud.dockerosb.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val appConfig: AppConfig
) : WebSecurityConfigurerAdapter() {

  override fun configure(auth: AuthenticationManagerBuilder) {
    val memory = auth.inMemoryAuthentication()
    memory.withUser(appConfig.basicAuthUsername).password(passwordEncoder().encode(appConfig.basicAuthPassword)).roles("USER", "ADMIN")
  }

  @Throws(Exception::class)
  override fun configure(http: HttpSecurity) {
    val entryPoint = BasicAuthenticationEntryPoint().apply {
      realmName = "GenericOSB"
    }

    http.csrf().disable()
        .authorizeRequests().anyRequest().authenticated()
        .and()
        .httpBasic().authenticationEntryPoint(entryPoint)
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder {
    return BCryptPasswordEncoder()
  }
}