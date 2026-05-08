plugins {
    id("configura.publish")
}

dependencies {
    "api"(project(":configura-api"))
    "implementation"(libs.bundles.jackson)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "configura-common"
            pom {
                name.set("configura-common")
                description.set("Shared implementation for Configura")
            }
        }
    }
}
