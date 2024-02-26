package io.github.thibaultbee.krtmp.rtmp

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

private const val RESOURCE_PATH = "./src/commonTest/resources"

fun ResourcePath(path: String) = Path("${RESOURCE_PATH}/${path}")

fun Resource(path: String) = Resource(ResourcePath(path))

class Resource(val path: Path) {
    fun toByteArray() = SystemFileSystem.source(path).buffered().readByteArray()

    fun toSource() = SystemFileSystem.source(path).buffered()
}