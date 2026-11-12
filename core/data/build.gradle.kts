plugins {
    id("lifetracker.android.library")
}

android {
    namespace = "com.example.lifet.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
}
