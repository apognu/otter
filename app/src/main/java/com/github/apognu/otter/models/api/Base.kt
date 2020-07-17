package com.github.apognu.otter.models.api

import kotlinx.serialization.*

@Serializable
data class OtterResponse<D : Any>(
  val count: Int,
  val next: String? = null,
  val results: List<D>
)

@Serializer(forClass = OtterResponse::class)
class OtterResponseSerializer<T : Any>(private val dataSerializer: KSerializer<T>) : KSerializer<OtterResponse<T>> {
  override val descriptor = PrimitiveDescriptor("OtterResponse", kind = PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: OtterResponse<T>) {}

  override fun deserialize(decoder: Decoder): OtterResponse<T> {
    return OtterResponse.serializer(dataSerializer).deserialize(decoder)
  }
}

@Serializable
data class Credentials(val token: String? = null, val non_field_errors: List<String>? = null)

@Serializable
data class User(val full_username: String)