package io.github.thibaultbee.krtmp.amf

import kotlinx.serialization.Serializable

@Serializable
data class DataBoolean(val value: Boolean)

@Serializable
data class DataNumber(val value: Double)

@Serializable
data class DataString(val value: String)

@Serializable
data class DataStrictArray(val value: List<String>)

@Serializable
data class DataEcmaArray(val value: Map<String, String>)