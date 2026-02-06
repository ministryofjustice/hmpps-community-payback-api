package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport

@Configuration
@EnableSpringDataWebSupport(
  pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO,
)
class SpringDataWebConfig
