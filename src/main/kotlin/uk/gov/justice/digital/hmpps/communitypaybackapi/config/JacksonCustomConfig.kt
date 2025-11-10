package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDurationDeserializer
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDurationSerializer

@Configuration
class JacksonCustomConfig {
  @Bean
  fun customModule(): SimpleModule {
    val module = SimpleModule()
    module.addSerializer(HourMinuteDuration::class.java, HourMinuteDurationSerializer())
    module.addDeserializer(HourMinuteDuration::class.java, HourMinuteDurationDeserializer())
    return module
  }
}
