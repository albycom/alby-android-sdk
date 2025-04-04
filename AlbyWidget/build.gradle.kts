import java.util.Base64

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.alby.widget"
    compileSdk = 34
    version = findProperty("VERSION_NAME") as String

    defaultConfig {
        minSdk = 23

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.okhttp)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = findProperty("GROUP") as String
                artifactId = findProperty("POM_ARTIFACT_ID") as String
                version = findProperty("VERSION_NAME") as String

                afterEvaluate {
                    from(components["release"])
                }
            }
        }

        repositories {
            maven {
                name = "mavenCentral"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                
                val tokenId = findProperty("mavenCentralUsername")?.toString() ?: return@maven
                val tokenSecret = findProperty("mavenCentralPassword")?.toString() ?: return@maven
                val encoded = Base64.getEncoder().encodeToString("$tokenId:$tokenSecret".toByteArray())

                credentials(HttpHeaderCredentials::class) {
                    name = "Authorization"
                    value = "Basic $encoded"
                }

                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }

    signing {
        val keyId = findProperty("signingInMemoryKeyId")?.toString() ?: return@signing
        val key = findProperty("signingInMemoryKey")?.toString() ?: return@signing
        val keyPassword = findProperty("signingInMemoryKeyPassword")?.toString() ?: return@signing
        
        sign(publishing.publications["maven"])
        useInMemoryPgpKeys(keyId, key, keyPassword)
    }
}