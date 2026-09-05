plugins {
    `maven-publish`
    id("java-conventions")
    id("testing")
}

publishing {
    repositories {
        maven {
            val base = providers.environmentVariable("PUBLISH_MAVEN_BASE_URL").orElse("https://registry.whereareiam.me/maven").get()
            val repository = providers.environmentVariable("PUBLISH_MAVEN_REPOSITORY").orElse("packages").get()
            url = uri("$base/$repository")
            credentials {
                username = providers.environmentVariable("PUBLISH_USER").orNull
                password = providers.environmentVariable("PUBLISH_TOKEN").orNull
            }
        }
    }
}
tasks.withType<Jar>().configureEach {
    archiveBaseName.set(provider {
        project.extensions.getByType<PublishingExtension>().publications.withType<MavenPublication>().single().artifactId
    })
}
