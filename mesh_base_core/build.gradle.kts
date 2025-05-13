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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
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
            create<MavenPublication>("maven") {
                groupId = groupId
                artifactId = artifactId
                version = version

                from(components["release"])
            }
        }
    }
}
