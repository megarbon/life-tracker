plugins {
    id("lifetracker.android.library")
    id("lifetracker.android.hilt")
}

android {
    namespace = "com.example.lifet.core.datastore"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
