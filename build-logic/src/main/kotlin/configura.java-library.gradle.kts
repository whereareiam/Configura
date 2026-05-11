import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
	`java-library`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "me.whereareiam"
version = providers.environmentVariable("VERSION").orElse("dev").get()

repositories {
	mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
	sourceCompatibility = JavaVersion.VERSION_17.toString()
	targetCompatibility = JavaVersion.VERSION_17.toString()
}

dependencies {
	"compileOnly"(libs.findLibrary("jetbrains-annotations").get())
	"testCompileOnly"(libs.findLibrary("jetbrains-annotations").get())
}
