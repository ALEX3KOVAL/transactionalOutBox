plugins {
    id("java-library")
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
}

group = "ru.alex3koval"
version = "1.0.0"

repositories {
    mavenCentral()
    loadEventingGithubPackages()
}

dependencies {
    implementation("org.springframework:spring-context:6.1.5")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.5.6")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.5.4")

    implementation("io.debezium:debezium-api:3.2.2.Final")
    implementation("io.debezium:debezium-core:3.2.2.Final")
    implementation("io.debezium:debezium-embedded:3.2.2.Final")
    implementation("io.debezium:debezium-connector-postgres:3.2.2.Final")

    implementation("io.projectreactor:reactor-core:3.4.40")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("alex3koval:eventing-contract:1.13.+") // TODO НУЖНО НАПИСАТЬ КАСТОМНЫЙ СТАРТЕР ДЛЯ РАБОТЫ С eventing
    implementation("alex3koval:eventing-impl:1.0.+") // TODO НУЖНО НАПИСАТЬ КАСТОМНЫЙ СТАРТЕР ДЛЯ РАБОТЫ С eventing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "alex3koval"
            artifactId = "transactional-outbox"
            version = "1.0.0"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ALEX3KOVAL/transactionalOutBox")

            credentials {
                username = "ALEX3KOVAL"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
