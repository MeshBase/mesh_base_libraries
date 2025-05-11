plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

group = "io.github.meshbase"
version = "1.1"

android {
    namespace = "io.github.meshbase.mesh_base_core"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Tell AGP to generate a 'release' software component for publishing
//    publishing {
//        singleVariant("release") {
//            withSourcesJar()
//        }
//    }
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.androidx.core)
    testImplementation(libs.mockito)
    mockitoAgent(libs.mockito) { isTransitive = false }
    testImplementation(libs.mockk)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
                artifact("$buildDir/outputs/aar/mesh_base_core-release.aar") {
                      extension = "aar"
                }
            }
        }
    }
}

//tasks.register("compileAndPublishToMaven") {
//    group = "publishing"
//    description = "Assembles the release AAR and publishes it to Maven Local"
//    dependsOn("assembleRelease")
//    dependsOn("publishReleasePublicationToMavenLocal")
//}

// Ensure the publish task depends on the AAR bundling task
