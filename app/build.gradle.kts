plugins {
    id("lifetracker.android.application")
    id("lifetracker.android.compose")
    id("lifetracker.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.lifet"

    defaultConfig {
        applicationId = "com.example.lifet"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    debugImplementation(project(":core:demo"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:finance"))
    implementation(project(":feature:fitness"))
    implementation(project(":feature:insights"))
    implementation(project(":feature:mood"))
    implementation(project(":feature:nutrition"))
    implementation(project(":feature:quit"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:work"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
