import org.gradle.api.tasks.compile.JavaCompile

plugins {
	`java-library`
}

group = "me.whereareiam"
version = providers.environmentVariable("VERSION").orElse("dev").get()

repositories {
	mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
	sourceCompatibility = JavaVersion.VERSION_17.toString()
	targetCompatibility = JavaVersion.VERSION_17.toString()
}
