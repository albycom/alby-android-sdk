plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
    signing
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

signing {
    val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
    val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
    val signingKeyId = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId")
    
    if (signingKey != null && signingPassword != null && signingKeyId != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    } else {
        logger.warn("Signing credentials not found. Artifacts will not be signed.")
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.findProperty("GROUP") as String
                artifactId = project.findProperty("POM_ARTIFACT_ID") as String
                version = project.findProperty("VERSION_NAME") as String

                pom {
                    name.set(project.findProperty("POM_NAME") as String)
                    description.set(project.findProperty("POM_DESCRIPTION") as String)
                    url.set(project.findProperty("POM_URL") as String)
                    licenses {
                        license {
                            name.set(project.findProperty("POM_LICENSE_NAME") as String)
                            url.set(project.findProperty("POM_LICENSE_URL") as String)
                            distribution.set(project.findProperty("POM_LICENSE_DIST") as String)
                        }
                    }
                    developers {
                        developer {
                            id.set(project.findProperty("POM_DEVELOPER_ID") as String)
                            name.set(project.findProperty("POM_DEVELOPER_NAME") as String)
                            url.set(project.findProperty("POM_DEVELOPER_URL") as String)
                        }
                    }
                    scm {
                        url.set(project.findProperty("POM_SCM_URL") as String)
                        connection.set(project.findProperty("POM_SCM_CONNECTION") as String)
                        developerConnection.set(project.findProperty("POM_SCM_DEV_CONNECTION") as String)
                    }
                }
            }
        }
    }

    // Sign after publications are created
    if (System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null) {
        signing.sign(publishing.publications["release"])
    }
}