plugins {
    id("facade")
}

dependencies {
    api(project(":configura-api"))
    implementation(project(":configura-common"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "configura"
            pom {
                name.set("configura")
                description.set("Convenient configuration loading, merging and defaults")
            }
        }
    }
}
