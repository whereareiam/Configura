dependencies {
    runtimeOnly(project(":configura-common"))
    testImplementation(libs.junit.jupiter)
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
