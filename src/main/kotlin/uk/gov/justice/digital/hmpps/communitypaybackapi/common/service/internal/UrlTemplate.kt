package uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.internal

import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.GenericConverter
import org.springframework.stereotype.Component

class UrlTemplate(val template: String) {
  fun resolve(args: Map<String, String>) = args.entries.fold(template) { acc, (key, value) -> acc.replace("#$key", value) }
}

@Component
class UrlTemplateConverter : GenericConverter {
  override fun getConvertibleTypes(): MutableSet<GenericConverter.ConvertiblePair> = mutableSetOf(GenericConverter.ConvertiblePair(String::class.java, UrlTemplate::class.java))

  override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any {
    val input = source as String
    return UrlTemplate(input)
  }
}
