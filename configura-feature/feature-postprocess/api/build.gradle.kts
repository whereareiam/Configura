plugins {
    id("publish")
}

group = "me.whereareiam.configura.feature"

dependencies {
    api(project(":configura-api"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "postprocess-api"
        }
    }
}
