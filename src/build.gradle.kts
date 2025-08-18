import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

kotlin {
    androidTarget()
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.accompanist.lyrics.core)
                implementation(compose.components.resources)
            }
        }
    }
}

compose {
    resources {
        packageOfResClass = "com.mocharealm.accompanist.lyrics.ui"
        publicResClass = true
        generateResClass = always
        customDirectory(
            sourceSetName = "commonMain",
            directoryProvider = provider {
                layout.projectDirectory.dir("src/commonMain/resources")
            }
        )
    }
}

android {
    namespace = "com.mocharealm.accompanist.lyrics.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true
        )
    )

    signAllPublications()

    coordinates("com.mocharealm.accompanist", "lyrics-ui", rootProject.version.toString())

    pom {
        name = "Accompanist Lyrics UI"
        description = "A lyrics displaying library for Compose Multiplatform"
        inceptionYear = "2025"
        url = "https://mocharealm.com/open-source"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "6xingyv"
                name = "Simon Scholz"
                url = "https://github.com/6xingyv"
            }
        }
        scm {
            url = "https://github.com/6xingyv/Accompanist"
            connection = "scm:git:git://github.com/6xingyv/Accompanist.git"
            developerConnection = "scm:git:ssh://git@github.com/6xingyv/Accompanist.git"
        }
    }
}
