plugins {

    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.android.application")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.fyp_prototype"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.fyp_prototype"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    packaging{
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    testImplementation(libs.testng)
    testImplementation(libs.junit.junit)
    val nav_version = "2.8.8"
    implementation(platform(libs.firebase.bom))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation(libs.firebase.database)
    implementation(libs.osmdroid)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.location)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation("androidx.navigation:navigation-compose:$nav_version")
    testImplementation ("androidx.test:core:1.5.0")
    testImplementation ("io.mockk:mockk-android:1.13.10")
    testImplementation ("org.robolectric:robolectric:4.10.3")
    androidTestImplementation ("io.mockk:mockk-android:1.13.10")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.1")
}