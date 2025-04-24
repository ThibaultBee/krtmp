package io.github.thibaultbee.krtmp.flv.tags

/**
 * Enum representing the type of multi track configuration.
 */
enum class MultitrackType(val value: Byte) {
    ONE_TRACK(0), MANY_TRACK(1), MANY_TRACK_MANY_CODEC(2);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid MultitrackType value: $value"
            )
    }
}