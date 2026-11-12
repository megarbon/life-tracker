plugins {
    id("lifetracker.android.library")
}

android {
    namespace = "com.example.lifet.core.testing"
}

dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
