plugins {
    id("feature")
}

group = "me.whereareiam.configura.feature"

dependencies {
    api(project(":configura-feature:feature-polymorphic:api"))
    implementation(project(":configura-api"))
    testImplementation(project(":configura"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "polymorphic"
        }
    }
}
