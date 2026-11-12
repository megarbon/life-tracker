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

    // Fakes de repositorios de cada área (ver paquete por área, p. ej. `nutrition/`).
    api(project(":core:model"))
    api(project(":core:common"))
    api(project(":core:data"))
}
