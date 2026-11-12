plugins {
    id("lifetracker.android.library")
}

android {
    namespace = "com.example.lifet.core.domain"
}

// Código de dominio sin APIs de Android; es librería Android sólo para poder depender de core:common.
dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(project(":core:data"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
