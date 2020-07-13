import org.jetbrains.kotlin.konan.properties.hasProperty
import java.io.FileInputStream
import java.util.*

plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-android-extensions")
  id("kotlin-kapt")
  id("realm-android")

  id("org.jlleitschuh.gradle.ktlint") version "8.1.0"
  id("com.gladed.androidgitversion") version "0.4.10"
  id("com.github.triplet.play") version "2.4.2"

  kotlin("plugin.serialization") version "1.3.70"
}

val props = Properties().apply {
  try {
    load(FileInputStream(rootProject.file("local.properties")))
  } catch (e: Exception) {
  }
}

androidGitVersion {
  codeFormat = "MNNNPPP"
  format = "%tag%%-count%%-commit%%-branch%"
}

android {
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8.toString()
  }

  buildToolsVersion = "29.0.3"
  compileSdkVersion(29)

  defaultConfig {
    applicationId = "com.github.apognu.otter"

    minSdkVersion(23)
    targetSdkVersion(29)

    ndkVersion = "21.3.6528147"

    versionCode = androidGitVersion.code()
    versionName = androidGitVersion.name()
  }

  signingConfigs {
    create("release") {
      if (props.hasProperty("signing.store")) {
        storeFile = file(props.getProperty("signing.store"))
        storePassword = props.getProperty("signing.store_passphrase")
        keyAlias = props.getProperty("signing.alias").toString()
        keyPassword = props.getProperty("signing.key_passphrase")
      }
    }
  }

  buildTypes {
    getByName("debug") {
      isDebuggable = true
      applicationIdSuffix = ".dev.livedata"
      manifestPlaceholders = mapOf(
        "app_name" to "Otter (livedata)"
      )

      resValue("string", "debug.hostname", props.getProperty("debug.hostname", ""))
      resValue("string", "debug.username", props.getProperty("debug.username", ""))
      resValue("string", "debug.password", props.getProperty("debug.password", ""))
    }

    getByName("release") {
      manifestPlaceholders = mapOf(
        "app_name" to "Otter"
      )

      if (props.hasProperty("signing.store")) {
        signingConfig = signingConfigs.getByName("release")
      }

      resValue("string", "debug.hostname", "")
      resValue("string", "debug.username", "")
      resValue("string", "debug.password", "")

      isMinifyEnabled = true
      isShrinkResources = true

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
}

ktlint {
  debug.set(false)
  verbose.set(false)
}

play {
  isEnabled = props.hasProperty("play.credentials")

  if (isEnabled) {
    serviceAccountCredentials = file(props.getProperty("play.credentials"))
    defaultToAppBundles = true
    track = "beta"
  }
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.72")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.7")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")

  implementation("androidx.appcompat:appcompat:1.2.0")
  implementation("androidx.core:core-ktx:1.5.0-alpha02")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.0-alpha07")
  implementation("androidx.fragment:fragment-ktx:1.2.5")
  implementation("androidx.room:room-runtime:2.2.5")
  implementation("androidx.room:room-ktx:2.2.5")
  implementation("androidx.paging:paging-runtime:3.0.0-alpha06")
  implementation("androidx.coordinatorlayout:coordinatorlayout:1.1.0")
  implementation("androidx.preference:preference:1.1.1")
  implementation("androidx.recyclerview:recyclerview:1.1.0")
  implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
  implementation("com.google.android.material:material:1.3.0-alpha02")
  implementation("com.android.support.constraint:constraint-layout:2.0.1")

  implementation("com.google.android.exoplayer:exoplayer-core:2.11.5")
  implementation("com.google.android.exoplayer:exoplayer-ui:2.11.5")
  implementation("com.google.android.exoplayer:extension-mediasession:2.11.5")

  implementation("com.aliassadi:power-preference-lib:1.4.1")
  implementation("com.github.kittinunf.fuel:fuel:2.2.3")
  implementation("com.github.kittinunf.fuel:fuel-coroutines:2.1.0")
  implementation("com.github.kittinunf.fuel:fuel-android:2.1.0")
  implementation("com.github.kittinunf.fuel:fuel-gson:2.1.0")
  implementation("com.github.kittinunf.fuel:fuel-kotlinx-serialization:2.2.3")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("com.squareup.picasso:picasso:2.71828")
  implementation("jp.wasabeef:picasso-transformations:2.2.1")

  debugImplementation("com.amitshekhar.android:debug-db:1.0.6")

  kapt("androidx.room:room-compiler:2.2.5")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
  kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ImplicitReflectionSerializer"
  kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.UnstableDefault"
}
