import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.kotlin.dsl.create
import java.net.URI

private fun RepositoryHandler.loadGithubPackage(repo: String) {
    maven {
        name = "GitHubPackages"
        url = URI("https://maven.pkg.github.com/ALEX3KOVAL/$repo")
        credentials {
            username = "ALEX3KOVAL"
            password = "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"
        }
        authentication {
            create<BasicAuthentication>("basic")
        }

        content {
            includeGroup("alex3koval")
        }
    }
}

fun RepositoryHandler.loadEventingGithubPackages() {
    listOf("eventingContract", "eventingImpl").forEach(::loadGithubPackage)
}
