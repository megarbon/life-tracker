plugins {
    id("lifetracker.android.library")
    id("lifetracker.android.room")
    id("lifetracker.android.hilt")
}

android {
    namespace = "com.example.lifet.core.database"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    sourceSets {
        // Los esquemas exportados sirven para validar migraciones en los tests.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
