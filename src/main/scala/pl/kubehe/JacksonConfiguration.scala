package pl.kubehe

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.quarkus.jackson.ObjectMapperCustomizer
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class JacksonConfiguration extends ObjectMapperCustomizer{
  override def customize(objectMapper: ObjectMapper): Unit = {
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    objectMapper.registerModule(DefaultScalaModule)
  }
}
