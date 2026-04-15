import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    // Kotlin 2.x이면 이거 추가 추천. Kotlin 2.0+에서는 Compose compiler가 Kotlin plugin과 함께 가는 방향
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jyco.smarttransfer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jyco.smarttransfer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions{
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {

    // ✅ Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.preview)
    implementation(libs.compose.material3)
    implementation(libs.google.material)
    implementation(libs.compose.navigation)
    implementation(libs.compose.viewmodel)

    debugImplementation(libs.compose.ui.tooling)

    // ✅ Koin BOM
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // ✅ Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ✅ Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ✅ Coroutines
    implementation(libs.coroutines.android)

    // Test Unit
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}