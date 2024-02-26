package io.github.thibaultbee.krtmp.rtmp

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

private const val RESOURCE_PATH = "./src/commonTest/resources"
class ResourcePath(path: String) {
    val path = Path("${RESOURCE_PATH}/${path}")
}

fun Resource(path: String) = Resource(ResourcePath(path))

class Resource(private val path: ResourcePath) {
    private val source = SystemFileSystem.source(path.path)
    fun toByteArray() = SystemFileSystem.source(path.path).buffered().readByteArray()

    fun toSource() = SystemFileSystem.source(path.path).buffered()
}