// Top-level build file where you can add configuration options common to all sub-projects/modules.
val maxSdkVers by extra(31)
val minSdkVers by extra(23)
val codeVers by extra(2)
val hiltVers by extra("2.40")

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")

        classpath("com.google.dagger:hilt-android-gradle-plugin:2.40")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register("clean",Delete::class){
    delete(rootProject.buildDir)
}

// detekt
plugins {
    id("io.gitlab.arturbosch.detekt").version("1.18.1")
}

val analysisDir = file(projectDir)
val analysisProjectDir = files("$rootDir/app/src/main/java", "$rootDir/app/src/main/kotlin")
val configDir = "$rootDir/config/detekt"
val reportDir = "$rootDir/app/build/detekt"
val configFile = file("$configDir/detekt.yml")
val baselineFile = file("$configDir/baseline.xml")
val statisticsConfigFile = file("$configDir/statistics.yml")

val kotlinFiles = "**/*.kt"
val kotlinScriptFiles = "**/*.kts"
val resourceFiles = "**/resources/**"
val buildFiles = "**/build/**"

val detektAll by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    description = "Runs the whole project at once."
    parallel = true
    allRules = true
    buildUponDefaultConfig = true
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
    group = "verification"
    reports {
        xml {
            enabled = true
            destination = file("$reportDir/detekt.xml")
        }
        html {
            enabled = true
            destination = file("$reportDir/detekt.html")
        }
        txt {
            enabled = true
            destination = file("$reportDir/detekt.txt")
        }
    }
}

val detektFormat by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    description = "Formats whole project."
    parallel = true
    disableDefaultRuleSets = true
    buildUponDefaultConfig = true
    autoCorrect = true
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
    reports {
        xml.enabled = false
        html.enabled = false
        txt.enabled = false
    }
}

val detektProjectBaseline by tasks.registering(io.gitlab.arturbosch.detekt.DetektCreateBaselineTask::class) {
    description = "Overrides current baseline."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
}
// detekt end
