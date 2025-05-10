plugins {
    alias(libs.plugins.android.library)
}

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
}

val mockitoAgent = configurations.create("mockitoAgent")
dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.mockito)
    mockitoAgent(libs.mockito) { isTransitive = false }
    testImplementation(libs.mockk)
}

tasks.register<Copy>("copyAarToFlutter") {
    from(layout.buildDirectory.file("outputs/aar/mesh_base_core-debug.aar"))
    into("$rootDir/mesh_base_flutter/android/libs/")
    dependsOn(tasks.named("assemble"))
}

afterEvaluate {
    tasks.named("assemble") {
        finalizedBy(tasks.named("copyAarToFlutter"))
    }
}
