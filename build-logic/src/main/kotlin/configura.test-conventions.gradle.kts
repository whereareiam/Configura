import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
	add("compileOnly", libs.findLibrary("lombok").get())
	add("annotationProcessor", libs.findLibrary("lombok").get())
	add("testCompileOnly", libs.findLibrary("lombok").get())
	add("testAnnotationProcessor", libs.findLibrary("lombok").get())

	add("testImplementation", libs.findLibrary("junit-jupiter").get())
	add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}
