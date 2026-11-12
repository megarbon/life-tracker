plugins {
    id("lifetracker.android.library")
}

android {
    namespace = "com.example.lifet.core.domain"
}

// Código de dominio sin APIs de Android; es librería Android sólo para poder depender de core:common.
// core:data es api porque los casos de uso exponen sus repositorios en constructores públicos.
dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(project(":core:data"))

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(project(":core:testing"))
}
