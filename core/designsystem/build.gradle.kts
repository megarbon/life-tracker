plugins {
    id("lifetracker.android.library")
    id("lifetracker.android.compose")
}

android {
    namespace = "com.example.lifet.core.designsystem"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
