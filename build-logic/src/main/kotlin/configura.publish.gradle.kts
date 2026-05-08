import org.gradle.api.publish.PublishingExtension

plugins {
	id("configura.java-library")
	id("configura.test-conventions")
	`maven-publish`
}

extensions.configure<PublishingExtension> {
	repositories {
		maven {
			val buildVersion = providers.environmentVariable("VERSION").orElse("dev").get()
			val realm = providers.environmentVariable("PUBLISH_REALM")
				.orElse(if (buildVersion.contains("dev", ignoreCase = true)) "development" else "release")
				.get()
				.lowercase()

			url = uri("https://maven.whereareiam.me/$realm")
			credentials {
				username = providers.environmentVariable("PUBLISH_USER").orNull.orEmpty()
				password = providers.environmentVariable("PUBLISH_TOKEN").orNull.orEmpty()
			}
		}
	}
}
