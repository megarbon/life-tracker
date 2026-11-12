plugins {
    id("lifetracker.android.library")
    id("lifetracker.android.compose")
}

android {
    namespace = "com.example.lifet.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:model"))
}
