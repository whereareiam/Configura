plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":configura-api"))
    runtimeOnly(project(":configura-common"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "configura"
            pom {
                name.set("configura")
                description.set("Configura bootstrap: API facade with runtime wiring")
            }
        }
    }
}