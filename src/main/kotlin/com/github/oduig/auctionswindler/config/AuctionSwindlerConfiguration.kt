package com.github.oduig.auctionswindler.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.github.oduig.auctionswindler.blizzard.BlizzardCredentials
import com.github.oduig.auctionswindler.config.properties.AuctionSwindlerConfigProperties
import com.github.oduig.auctionswindler.config.properties.CorsConfigProperties
import com.github.oduig.auctionswindler.config.properties.WebClientConfigProperties
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mu.KLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.zalando.problem.ProblemModule
import org.zalando.problem.validation.ConstraintViolationProblemModule
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient

@Configuration
@EnableConfigurationProperties(AuctionSwindlerConfigProperties::class)
//@EnableScheduling
//@EnableAsync
class AuctionSwindlerConfiguration() {

  companion object: KLogging()

  // Prepare sub configurations for easier auto wiring
  @Bean
  fun cors(configProperties: AuctionSwindlerConfigProperties): CorsConfigProperties =
      configProperties.cors!!

  @Bean
  fun webClient(configProperties: AuctionSwindlerConfigProperties): WebClientConfigProperties =
      configProperties.webClient!!

  @Bean
  fun springWebClient(webClientConfigProperties: WebClientConfigProperties): WebClient {
    val tcpClient = TcpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientConfigProperties.connectTimeoutMs!!)
        .doOnConnected { connection ->
          connection.addHandlerLast(ReadTimeoutHandler(webClientConfigProperties.requestTimeoutSeconds!!))
              .addHandlerLast(WriteTimeoutHandler(webClientConfigProperties.requestTimeoutSeconds!!))
        }

    return WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(HttpClient.from(tcpClient)))
        .build()
  }

  @Bean
  @Primary
  fun json(configProperties: AuctionSwindlerConfigProperties): ObjectMapper = ObjectMapper()
      .registerModule(KotlinModule())
      .registerModule(ParameterNamesModule())
      .registerModule(Jdk8Module())
      .registerModule(JavaTimeModule())
      .registerModule(ProblemModule().withStackTraces())
      .registerModule(ConstraintViolationProblemModule())
      .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
      .configure(SerializationFeature.INDENT_OUTPUT, configProperties.prettyJson)
      .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
      .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)

  @Bean
  fun blizzardCredentials(): BlizzardCredentials {
    val clientId = System.getenv("BLIZZ_API_CLIENT_ID")
    val clientSecret = System.getenv("BLIZZ_API_CLIENT_SECRET")
    require(clientId.isNotBlank())
    requireNotNull(clientSecret.isNotBlank())
    return BlizzardCredentials(clientId, clientSecret)
  }

// Async config
//  @Bean
//  fun webClient(configProperties: AuctionSwindlerConfigProperties): WebClient {
//    return WebClient.builder()
//        .clientConnector(ReactorClientHttpConnector {
//          it.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configProperties.webClientConnectTimeoutMs)
//        }).build()
//  }

// Database config
//  @Bean
//  fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
//    return JdbcTemplate(dataSource)
//  }

// Scheduler config
//  /**
//   * Cleans up the following log message:
//   * "No TaskScheduler/ScheduledExecutorService bean found for scheduled processing"
//   */
//  @Bean
//  fun scheduledExecutorService(): ScheduledExecutorService {
//    return Executors.newSingleThreadScheduledExecutor()
//  }
//
//  @Bean
//  fun scheduler(scheduledExecutorService: ScheduledExecutorService): ConcurrentTaskScheduler {
//    return ConcurrentTaskScheduler(scheduledExecutorService)
//  }
}
