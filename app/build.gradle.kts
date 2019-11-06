import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.konan.properties.hasProperty
import java.io.FileInputStream
import java.util.*

plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-android-extensions")

  id("org.jlleitschuh.gradle.ktlint") version "8.1.0"
  id("com.gladed.androidgitversion") version "0.4.10"
  id("com.github.triplet.play") version "2.4.2"
}

val props = Properties().apply {
  try { load(FileInputStream(rootProject.file("local.properties"))) }
  catch(e: Exception) {}
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
    (this as KotlinJvmOptions).apply {
      jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
  }

  buildToolsVersion = "29.0.2"
  compileSdkVersion(29)

  defaultConfig {
    applicationId = "com.github.apognu.otter"

    minSdkVersion(23)
    targetSdkVersion(29)

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
    getByName("release") {
      if (props.hasProperty("signing.store")) {
        signingConfig = signingConfigs.getByName("release")
      }

      isMinifyEnabled = false

      proguardFile(getDefaultProguardFile("proguard-android-optimize.txt"))
      proguardFile("proguard-rules.pro")
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
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.50")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.2")

  implementation("androidx.appcompat:appcompat:1.1.0")
  implementation("androidx.core:core-ktx:1.2.0-beta01")
  implementation("androidx.coordinatorlayout:coordinatorlayout:1.0.0")
  implementation("androidx.preference:preference:1.1.0")
  implementation("androidx.recyclerview:recyclerview:1.0.0")
  implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.0.0")
  implementation("com.google.android.material:material:1.2.0-alpha01")
  implementation("com.android.support.constraint:constraint-layout:1.1.3")

  implementation("com.google.android.exoplayer:exoplayer:2.10.5")
  implementation("com.google.android.exoplayer:extension-mediasession:2.10.6")
  implementation("com.google.android.exoplayer:extension-cast:2.10.6")
  implementation("com.github.PaulWoitaschek.ExoPlayer-Extensions:extension-flac:2.10.5") {
    isTransitive = false
  }
  implementation("com.aliassadi:power-preference-lib:1.4.1")
  implementation("com.github.kittinunf.fuel:fuel:2.1.0")
  implementation("com.github.kittinunf.fuel:fuel-coroutines:2.1.0")
  implementation("com.github.kittinunf.fuel:fuel-android:2.1.0")
  implementation("com.github.kittinunf.fuel:fuel-gson:2.1.0")
  implementation("com.google.code.gson:gson:2.8.5")
  implementation("com.squareup.picasso:picasso:2.71828")
  implementation("jp.wasabeef:picasso-transformations:2.2.1")
}
