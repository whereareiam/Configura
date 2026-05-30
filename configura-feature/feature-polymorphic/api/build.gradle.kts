plugins {
    id("feature-api")
}

group = "me.whereareiam.configura.feature"

dependencies {
    api(project(":configura-api"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "polymorphic-api"
        }
    }
}
