rootProject.name = "Configura"

pluginManagement {
	includeBuild("build-logic")
}

include("configura-api")
include("configura-common")
include("configura")
include("configura-feature")
include("configura-feature:feature-extension")
include("configura-feature:feature-extension:api")
include("configura-feature:feature-polymorphic")
include("configura-feature:feature-polymorphic:api")
include("configura-feature:feature-postprocess")
include("configura-feature:feature-postprocess:api")

project(":configura-feature").projectDir = file("configura-feature")
project(":configura-feature:feature-extension").projectDir = file("configura-feature/feature-extension")
project(":configura-feature:feature-extension:api").projectDir = file("configura-feature/feature-extension/api")
project(":configura-feature:feature-polymorphic").projectDir = file("configura-feature/feature-polymorphic")
project(":configura-feature:feature-polymorphic:api").projectDir = file("configura-feature/feature-polymorphic/api")
project(":configura-feature:feature-postprocess").projectDir = file("configura-feature/feature-postprocess")
project(":configura-feature:feature-postprocess:api").projectDir = file("configura-feature/feature-postprocess/api")
