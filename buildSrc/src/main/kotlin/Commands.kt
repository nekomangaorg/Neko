import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.of
import org.gradle.process.ExecOperations

// Git is needed in your system PATH for these commands to work.
// If it's not installed, you can return a random value as a workaround

fun getBuildTime() = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now(ZoneOffset.UTC))
fun Project.getCommitCount() = providers.of(GitCommitCount::class) {}.get()
fun Project.getGitSha() = providers.of(GitSha::class) {}.get()

abstract class GitSha : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations
    override fun obtain(): String {
        val byteOut = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = byteOut
        }
        return String(byteOut.toByteArray()).trim()
    }
}

abstract class GitCommitCount : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations
    override fun obtain(): String {
        val byteOut = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = byteOut
        }
        return String(byteOut.toByteArray()).trim()
    }
}

