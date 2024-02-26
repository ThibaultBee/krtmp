import org.gradle.api.Project
import java.util.Locale

fun Project.loadProperty(
    projectPropertyName: String,
    envName: String = projectPropertyName.uppercase()
): String {
    val envValue = System.getenv(envName)?.toString()
    if (envValue != null) return envValue

    val projectPropertiesValue = project.properties[projectPropertyName]?.toString()
    if (projectPropertiesValue != null) return projectPropertiesValue

    return ""
}

fun Project.loadFileContents(
    projectPropertyName: String,
    envName: String = projectPropertyName.uppercase()
): String {
    val decodeIfNeeded: (String) -> String = {
        if (it.startsWith("~/")) {
            // the value is a path to file on disk. Read its contents
            val filePath = it.replace("~/", System.getProperty("user.home") + "/")
            project.file(filePath).readText()
        } else {
            // the value itself is the file contents file
            it
        }
    }

    val envValue = System.getenv(envName)?.toString()
    if (envValue != null) return decodeIfNeeded(envValue)

    val projectPropertiesValue = project.properties[projectPropertyName]?.toString()
    if (projectPropertiesValue != null) return decodeIfNeeded(projectPropertiesValue)

    return ""
}