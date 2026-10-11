plugins {
    id("publish")
}

group = "me.whereareiam.configura.feature"

dependencies {
    api(project(":configura-feature:feature-comment:api"))
    implementation(project(":configura-api"))
    implementation(project(":configura-common"))
    implementation(libs.jackson.yaml)
    testImplementation(project(":configura"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "comment"
        }
    }
}
