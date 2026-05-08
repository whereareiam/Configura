plugins {
    id("configura.publish")
}

dependencies {
    api(libs.jackson)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "configura-api"
            pom {
                name.set("configura-api")
                description.set("Public API for Configura")
            }
        }
    }
}
