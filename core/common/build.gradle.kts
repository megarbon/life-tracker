plugins {
    id("lifetracker.android.library")
    id("lifetracker.android.hilt")
}

android {
    namespace = "com.example.lifet.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)

    testImplementation(libs.kotlinx.coroutines.test)
}
