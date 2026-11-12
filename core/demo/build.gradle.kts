plugins {
    id("lifetracker.android.library")
    id("lifetracker.android.hilt")
}

android {
    namespace = "com.example.lifet.core.demo"
}

// Sólo debug: la app lo incluye con debugImplementation, así los datos de prueba nunca llegan a release.
dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))

    testImplementation(project(":core:testing"))
}
