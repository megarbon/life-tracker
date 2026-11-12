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

    testImplementation(libs.kotlinx.coroutines.test)
}
