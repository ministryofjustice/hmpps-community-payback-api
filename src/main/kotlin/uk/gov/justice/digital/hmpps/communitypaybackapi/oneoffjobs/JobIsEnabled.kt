package uk.gov.justice.digital.hmpps.communitypaybackapi.oneoffjobs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.get
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.core.type.MethodMetadata
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.ONE_OFF_JOBS_CONFIG_NAMESPACE

class JobIsEnabled : Condition {
  override fun matches(
    context: ConditionContext,
    metadata: AnnotatedTypeMetadata,
  ): Boolean = when (metadata) {
    is MethodMetadata -> context.environment["${ONE_OFF_JOBS_CONFIG_NAMESPACE}.jobs.${metadata.beanName}.enabled"].toBoolean()
    else -> false
  }

  private val MethodMetadata.beanName: String
    get() {
      val beanAnnotation = this.annotations.get(Bean::class.java)

      if (beanAnnotation.isPresent) {
        if (beanAnnotation.hasNonDefaultValue("value")) {
          return beanAnnotation.getStringArray("value")[0]
        }

        if (beanAnnotation.hasNonDefaultValue("name")) {
          return beanAnnotation.getStringArray("name")[0]
        }
      }

      return this.methodName
    }
}
