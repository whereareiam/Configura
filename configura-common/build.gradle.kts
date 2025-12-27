dependencies {
    "compileOnly"(project(":configura-api"))
    "compileOnly"(libs.bundles.jackson)

    "testImplementation"(project(":configura-api"))
    "testImplementation"(libs.junit.jupiter)
    "testRuntimeOnly"(libs.junit.platform.launcher)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "configura-common"
            pom {
                name.set("configura-common")
                description.set("Internal runtime for Configura")
            }
        }
    }
}