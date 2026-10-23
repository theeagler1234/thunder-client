// Eaglercraft 26.2 — Optimized root build configuration
// Enables configuration cache, build cache, parallel execution, and incremental compilation

import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" apply false
}

// ============================================================
// GLOBAL BUILD OPTIMIZATIONS
// ============================================================

// Enable configuration cache (caches the result of configuration phase)
gradle.configurationCache.set(true)

// Enable build cache (caches task outputs)
gradle.buildCache.set(true)

// Use parallel execution by default (can be overridden with --no-parallel)
gradle.startParameter.isParallel = true

// ============================================================
// JAVA TOOLCHAIN: Java 25 for compilation, emit Java 24 bytecode
// ============================================================
subprojects {
    // Apply common plugins
    apply(plugin = "java")

    repositories {
        maven("https://libraries.minecraft.net")
        mavenCentral()
    }

    // Configure Java toolchain - use JDK 25, emit classfile version 24 (compatible with TeaVM)
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Emit oldest bytecode level source accepts (classfile 68 = Java 24)
        // This ensures TeaVM (which supports up to Java 24 classfiles) can link the output
        options.release.set(providers.gradleProperty("eaglerJavaRelease").map(String::toInt).orElse(24))
        options.compilerArgs.add("-nowarn")
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
        // Decompiled sources: no annotation processing wanted
        options.compilerArgs.add("-proc:none")

        // Incremental compilation: only recompile changed files and their dependents
        isIncremental = true
    }

    // Optimize test tasks (if any)
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        isCacheable = true
    }
}

// ============================================================
// PROJECT DEFINITIONS
// ============================================================
rootProject.name = "eagler-26_2"

include("game")
include("platform")
include("platform-lwjgl")
include("target_lwjgl_desktop")
include("target_teavm_spike")
include("teavm-compat")
include("platform-teavm")
include("target_teavm")
include("target_teavm_wasm_gc")
include("target_teavm_wasm_gc_mesh")
include("target_teavm_wasm_gc_server")

// ============================================================
// CONVENIENCE TASKS FOR INCREMENTAL DEVELOPMENT
// ============================================================

// Quick compile single module: ./gradlew :game:compileJava
// Quick compile all: ./gradlew compileJava
// Quick test single module: ./gradlew :game:test
// Quick check (compile + test): ./gradlew :game:check

tasks.register("compileAllJava") {
    group = "eaglercraft build"
    description = "Compile all Java modules (incremental)"
    dependsOn(subprojects.map { it.tasks.named("compileJava") })
}

tasks.register("compileGameOnly") {
    group = "eaglercraft build"
    description = "Compile only the game module (fastest for code changes)"
    dependsOn(":game:compileJava")
}

tasks.register("compileDesktopOnly") {
    group = "eaglercraft build"
    description = "Compile desktop target (game + platform + platform-lwjgl)"
    dependsOn(":platform:compileJava", ":game:compileJava", ":platform-lwjgl:compileJava", ":target_lwjgl_desktop:compileJava")
}

tasks.register("compileWebOnly") {
    group = "eaglercraft build"
    description = "Compile web target (game + platform + platform-teavm + teavm-compat)"
    dependsOn(":platform:compileJava", ":game:compileJava", ":teavm-compat:compileJava", ":platform-teavm:compileJava", ":target_teavm:compileJava")
}

tasks.register("compileWasmGCOptimized") {
    group = "eaglercraft build"
    description = "Compile WasmGC target (game + platform + platform-teavm + teavm-compat + target_teavm_wasm_gc)"
    dependsOn(":platform:compileJava", ":game:compileJava", ":teavm-compat:compileJava", ":platform-teavm:compileJava", ":target_teavm_wasm_gc:compileJava")
}

// ============================================================
// DEVELOPMENT WORKFLOW TASKS
// ============================================================

tasks.register("devCompile") {
    group = "eaglercraft dev"
    description = "Fast development compile (game module only, no annotation processing)"
    dependsOn(":game:compileJava")
    // Skip annotation processing for faster dev iterations
    doLast {
        println("✓ Dev compile complete. Run :target_lwjgl_desktop:eaglercraftDebugRuntime for desktop")
        println("  Or build web targets with: ./gradlew :target_teavm:teavmBuild")
    }
}

tasks.register("devDesktop") {
    group = "eaglercraft dev"
    description = "Build and run desktop client (development mode)"
    dependsOn(":target_lwjgl_desktop:eaglercraftDebugRuntime")
}

tasks.register("devWeb") {
    group = "eaglercraft dev"
    description = "Build web JS client (development mode, fast link)"
    dependsOn(":target_teavm:teavmBuild")
    doLast {
        println("✓ Web build complete. Output in target_teavm/build/teavm/classes.js")
    }
}

tasks.register("devWasmGC") {
    group = "eaglercraft dev"
    description = "Build WasmGC client (development mode, fast link)"
    dependsOn(":target_teavm_wasm_gc:generateWasmGCIncremental")
    doLast {
        println("✓ WasmGC build complete. Output in target_teavm_wasm_gc/build/generated/teavm/wasm-gc/classes.wasm")
    }
}

// ============================================================
// CLEANUP & MAINTENANCE TASKS
// ============================================================

tasks.register("cleanAllCaches") {
    group = "eaglercraft maintenance"
    description = "Clean all Gradle and build caches"
    doLast {
        delete(rootProject.layout.buildDirectory.dir("teavm-incremental"))
        delete(rootProject.layout.buildDirectory.dir("teavm-cache"))
        println("✓ Build caches cleaned")
    }
}

// ============================================================
// PERFORMANCE TUNING (applied to all subprojects)
// ============================================================
subprojects {
    // Configure JVM arguments for compilation daemons
    tasks.withType<JavaCompile>().configureEach {
        // Use more memory for large compilation units
        options.forkOptions.with {
            jvmArgs = listOf("-Xmx2G", "-XX:+UseZGC", "-XX:+ParallelRefProcEnabled")
        }
    }
}