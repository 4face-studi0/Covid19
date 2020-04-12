@file:Suppress("LocalVariableName")

import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import studio.forface.easygradle.dsl.`coroutines-core-common`
import studio.forface.easygradle.dsl.`coroutines-core-js`
import studio.forface.easygradle.dsl.`coroutines-core`
import studio.forface.easygradle.dsl.`kotlin-common`
import studio.forface.easygradle.dsl.`kotlin-jdk8`
import studio.forface.easygradle.dsl.`kotlin-js`
import studio.forface.easygradle.dsl.`kotlin-test-junit`
import studio.forface.easygradle.dsl.`kotlin-test`
import studio.forface.easygradle.dsl.`serialization-common`
import studio.forface.easygradle.dsl.mockk

plugins {
    kotlin(PluginsDeps.multiplatform)
    id(PluginsDeps.kotlinSerialization)
}

kotlin {
    /* Targets configuration omitted.
     * To find out how to configure the targets, please follow the link:
     * https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    jvm()
    js()

    sourceSets {
        all {
            listOf(
                "coroutines.ExperimentalCoroutinesApi",
                "experimental.ExperimentalTypeInference",
                "time.ExperimentalTime"
            ).forEach { languageSettings.useExperimentalAnnotation("kotlin.$it") }
        }

        with(dependencyHandler) {

            val commonMain by getting {
                dependencies {
                    api(
                        `kotlin-common`,
                        `coroutines-core-common`,
                        `koin`
                    )

                    implementation(
                        `serialization-common`,
                        `klock`
                    )
                }
            }
            val commonTest by getting {
                dependencies {
                    api(project(Module.sharedTest))
                }
            }


            val jvmMain by getting {
                dependencies {
                    api(
                        `kotlin-jdk8`,
                        `coroutines-core`
                    )
                }
            }
            val jvmTest by getting {
                dependencies {
                    api(
                        `kotlin-test`,
                        `kotlin-test-junit`,
                        `mockk`
                    )
                }
            }

            js().compilations["main"].defaultSourceSet {
                dependencies {
                    api(
                        `kotlin-js`,
                        `coroutines-core-js`
                    )
                }
            }

        }
    }
}

// Configuration accessors
fun KotlinDependencyHandler.implementation(vararg dependencyNotations: Any) {
    for (dep in dependencyNotations) implementation(dep)
}

fun KotlinDependencyHandler.api(vararg dependencyNotations: Any) {
    for (dep in dependencyNotations) api(dep)
}
