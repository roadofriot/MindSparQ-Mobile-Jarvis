plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("app.cash.sqldelight")
}

android {
    namespace = "com.mindsparq.jarvis.core.database"
    compileSdk = 36
    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    implementation(project(":core:model"))
    implementation("app.cash.sqldelight:runtime:2.0.2")
    implementation("app.cash.sqldelight:android-driver:2.0.2")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("app.cash.sqldelight:sqlite-driver:2.0.2")
}

sqldelight {
    databases {
        create("JarvisDatabase") {
            packageName.set("com.mindsparq.jarvis.core.database")
        }
    }
}
