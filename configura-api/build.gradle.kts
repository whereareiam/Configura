dependencies {
    runtimeOnly(project(":configura-common"))
    testImplementation(libs.junit.jupiter)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "configura"
            pom {
                name.set("configura")
                description.set("Public API for Configura")
            }
        }
    }
}
