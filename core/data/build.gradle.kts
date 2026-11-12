plugins {
    id("lifetracker.android.library")
    id("lifetracker.android.hilt")
}

android {
    namespace = "com.example.lifet.core.data"
}

// Las interfaces de repositorio son públicas; las implementaciones son internal y se enlazan con Hilt.
dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(project(":core:database"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
