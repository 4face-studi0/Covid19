import com.android.build.gradle.TestedExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import studio.forface.easygradle.dsl.android.Version
import studio.forface.easygradle.dsl.android.version

fun Project.android(

    appIdSuffix: String? = null,
    minSdk: Int = 23,
    targetSdk: Int = 29

) = (this as ExtensionAware).extensions.configure<TestedExtension>("android") {

    compileSdkVersion(29)
    defaultConfig {
        appIdSuffix?.let { applicationId = "studio.forface.covid.android.$it" }
        version = Version(0, 1)
        minSdkVersion(minSdk)
        targetSdkVersion(targetSdk)
    }

    // Add support for `src/x/kotlin` instead of `src/x/java` only
    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = sourceCompatibility
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/*.kotlin_module")
    }
}
