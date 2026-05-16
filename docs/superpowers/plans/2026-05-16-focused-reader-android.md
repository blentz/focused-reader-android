# Focused Reader (Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android RSVP speed-reader (Kotlin, Compose, minSdk 30) per the design spec at `docs/superpowers/specs/2026-05-16-focused-reader-android-design.md`.

**Architecture:** Single-activity Compose app, MVVM with Hilt DI. Modularized internally by package: `data`, `reader`, `capture`, `ui` (home/settings/theme), `nav`, `di`. Coroutines + `StateFlow` drive a single `ReaderState`. Room single-slot session row + DataStore Preferences for settings.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, DataStore, Coroutines, JUnit5, Turbine, Espresso, Compose UI Test. AGP latest stable, Compose BOM, version catalog.

**Local Tooling:** Android SDK at `/home/blentz/Android`. Add `local.properties` with `sdk.dir=/home/blentz/Android` if running Gradle locally. CI not required for POC.

---

## Phase 0 — Project Scaffold

### Task 1: Initialize Gradle project structure

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `local.properties` (gitignored — but write a template `local.properties.example`)
- Create: `.gitignore`

- [ ] **Step 1: Write `.gitignore`**

```
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
app/build/
*.apk
*.aab
```

- [ ] **Step 2: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 3: Write `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
compose-bom = "2024.09.02"
activity-compose = "1.9.2"
lifecycle = "2.8.6"
navigation-compose = "2.8.1"
hilt = "2.52"
hilt-navigation-compose = "1.2.0"
room = "2.6.1"
datastore = "1.1.1"
coroutines = "1.9.0"
junit5 = "5.11.0"
turbine = "1.1.0"
mockk = "1.13.12"
androidx-test = "1.6.1"
espresso = "3.6.1"
core-ktx = "1.13.1"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "core-ktx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation-compose" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit5" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidx-test" }
androidx-test-rules = { module = "androidx.test:rules", version.ref = "androidx-test" }
espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 4: Write root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 5: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FocusedReader"
include(":app")
```

- [ ] **Step 6: Write `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.focusedreader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.focusedreader"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
}
```

- [ ] **Step 7: Write `local.properties.example`**

```properties
sdk.dir=/home/blentz/Android
```

- [ ] **Step 8: Generate Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.10`
Expected: Creates `gradlew`, `gradlew.bat`, `gradle/wrapper/` files. If `gradle` not installed, manually copy wrapper from any AGP 8.5+ project or use `/home/blentz/Android/cmdline-tools/latest/bin/sdkmanager` to install one.

- [ ] **Step 9: Commit**

```bash
git add .gitignore gradle.properties gradle/ settings.gradle.kts build.gradle.kts app/build.gradle.kts local.properties.example gradlew gradlew.bat
git commit -m "chore: gradle scaffold (kts, version catalog, compose, hilt, room)"
```

---

### Task 2: AndroidManifest + Application class

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/focusedreader/FocusedReaderApp.kt`
- Create: `app/src/main/java/com/focusedreader/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Write `strings.xml`**

```xml
<resources>
    <string name="app_name">Focused Reader</string>
</resources>
```

- [ ] **Step 2: Write `themes.xml`** (Compose handles theming; manifest needs an Android theme reference)

```xml
<resources>
    <style name="Theme.FocusedReader" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 3: Write `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:name=".FocusedReaderApp"
        android:label="@string/app_name"
        android:theme="@style/Theme.FocusedReader"
        android:allowBackup="false"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

- [ ] **Step 4: Write `FocusedReaderApp.kt`**

```kotlin
package com.focusedreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FocusedReaderApp : Application()
```

- [ ] **Step 5: Write `MainActivity.kt`**

```kotlin
package com.focusedreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface { Text("Focused Reader") }
        }
    }
}
```

- [ ] **Step 6: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main
git commit -m "chore: manifest, application class, MainActivity skeleton"
```

---

## Phase 1 — Reader Core (no UI yet)

### Task 3: Word tokenizer

**Files:**
- Create: `app/src/main/java/com/focusedreader/reader/WordTokenizer.kt`
- Test: `app/src/test/java/com/focusedreader/reader/WordTokenizerTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WordTokenizerTest {
    @Test fun `splits on whitespace`() {
        assertEquals(listOf("hello", "world"), WordTokenizer.tokenize("hello world"))
    }
    @Test fun `collapses repeated whitespace`() {
        assertEquals(listOf("a", "b"), WordTokenizer.tokenize("a   \t \n b"))
    }
    @Test fun `preserves punctuation attached to words`() {
        assertEquals(listOf("Hello,", "world!"), WordTokenizer.tokenize("Hello, world!"))
    }
    @Test fun `empty input returns empty list`() {
        assertEquals(emptyList<String>(), WordTokenizer.tokenize(""))
    }
    @Test fun `whitespace-only input returns empty list`() {
        assertEquals(emptyList<String>(), WordTokenizer.tokenize("   \n\t  "))
    }
}
```

- [ ] **Step 2: Run test, verify FAIL** (unresolved reference)

Run: `./gradlew :app:testDebugUnitTest --tests "*.WordTokenizerTest"`

- [ ] **Step 3: Implement**

```kotlin
package com.focusedreader.reader

object WordTokenizer {
    private val ws = Regex("\\s+")
    fun tokenize(text: String): List<String> =
        text.split(ws).filter { it.isNotBlank() }
}
```

- [ ] **Step 4: Run test, verify PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*.WordTokenizerTest"`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusedreader/reader/WordTokenizer.kt app/src/test/java/com/focusedreader/reader/WordTokenizerTest.kt
git commit -m "feat(reader): WordTokenizer with whitespace split"
```

---

### Task 4: ORP calculator

**Files:**
- Create: `app/src/main/java/com/focusedreader/reader/OrpCalculator.kt`
- Test: `app/src/test/java/com/focusedreader/reader/OrpCalculatorTest.kt`

- [ ] **Step 1: Write failing test (table-driven Spritz formula)**

```kotlin
package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class OrpCalculatorTest {
    private val cases = listOf(
        1 to 0,
        2 to 1, 3 to 1, 4 to 1, 5 to 1,
        6 to 2, 7 to 2, 8 to 2, 9 to 2,
        10 to 3, 11 to 3, 12 to 3, 13 to 3,
        14 to 4, 20 to 4, 50 to 4
    )

    @TestFactory
    fun `pivot index per length bucket`() = cases.map { (len, expected) ->
        DynamicTest.dynamicTest("len=$len → $expected") {
            assertEquals(expected, OrpCalculator.pivotIndex(len))
        }
    }
}
```

- [ ] **Step 2: Run test, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "*.OrpCalculatorTest"`

- [ ] **Step 3: Implement**

```kotlin
package com.focusedreader.reader

object OrpCalculator {
    fun pivotIndex(length: Int): Int = when {
        length <= 1 -> 0
        length <= 5 -> 1
        length <= 9 -> 2
        length <= 13 -> 3
        else -> 4
    }

    data class Split(val left: String, val pivot: Char, val right: String)

    fun split(word: String): Split {
        require(word.isNotEmpty()) { "Cannot split empty word" }
        val idx = pivotIndex(word.length)
        return Split(word.substring(0, idx), word[idx], word.substring(idx + 1))
    }
}
```

- [ ] **Step 4: Add Split test**

Append to `OrpCalculatorTest.kt`:

```kotlin
    @org.junit.jupiter.api.Test
    fun `split returns left pivot right`() {
        val s = OrpCalculator.split("reading")
        assertEquals("re", s.left)
        assertEquals('a', s.pivot)
        assertEquals("ding", s.right)
    }
    @org.junit.jupiter.api.Test
    fun `split single char`() {
        val s = OrpCalculator.split("a")
        assertEquals("", s.left); assertEquals('a', s.pivot); assertEquals("", s.right)
    }
```

- [ ] **Step 5: Run all tests, verify PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*.OrpCalculatorTest"`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/focusedreader/reader/OrpCalculator.kt app/src/test/java/com/focusedreader/reader/OrpCalculatorTest.kt
git commit -m "feat(reader): OrpCalculator with Spritz pivot formula"
```

---

### Task 5: WPM → delay math

**Files:**
- Create: `app/src/main/java/com/focusedreader/reader/Wpm.kt`
- Test: `app/src/test/java/com/focusedreader/reader/WpmTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WpmTest {
    @Test fun `100 wpm = 600ms per tick`() {
        assertEquals(600L, Wpm.tickMillis(100))
    }
    @Test fun `300 wpm = 200ms per tick`() {
        assertEquals(200L, Wpm.tickMillis(300))
    }
    @Test fun `900 wpm = 66ms per tick`() {
        assertEquals(66L, Wpm.tickMillis(900))
    }
    @Test fun `clamp respects bounds`() {
        assertEquals(100, Wpm.clamp(50, max = 900))
        assertEquals(900, Wpm.clamp(1500, max = 900))
        assertEquals(500, Wpm.clamp(2000, max = 500))
        assertEquals(300, Wpm.clamp(300, max = 900))
    }
}
```

- [ ] **Step 2: Run test, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "*.WpmTest"`

- [ ] **Step 3: Implement**

```kotlin
package com.focusedreader.reader

object Wpm {
    const val MIN = 100
    const val DEFAULT_MAX = 900
    fun tickMillis(wpm: Int): Long = 60_000L / wpm
    fun clamp(wpm: Int, max: Int = DEFAULT_MAX): Int = wpm.coerceIn(MIN, max)
}
```

- [ ] **Step 4: Run test, verify PASS**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusedreader/reader/Wpm.kt app/src/test/java/com/focusedreader/reader/WpmTest.kt
git commit -m "feat(reader): WPM clamp + tick delay math"
```

---

### Task 6: ReaderState model

**Files:**
- Create: `app/src/main/java/com/focusedreader/reader/ReaderState.kt`

- [ ] **Step 1: Implement (no test — pure data)**

```kotlin
package com.focusedreader.reader

sealed interface ReaderState {
    data object Idle : ReaderState
    data class Reading(val tokens: List<String>, val index: Int, val wpm: Int) : ReaderState
    data class Paused(val tokens: List<String>, val index: Int, val wpm: Int) : ReaderState
    data class Resuming(val tokens: List<String>, val index: Int, val wpm: Int, val secondsLeft: Int) : ReaderState
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/focusedreader/reader/ReaderState.kt
git commit -m "feat(reader): ReaderState sealed interface"
```

---

### Task 7: RSVP engine (tick loop)

**Files:**
- Create: `app/src/main/java/com/focusedreader/reader/RsvpEngine.kt`
- Test: `app/src/test/java/com/focusedreader/reader/RsvpEngineTest.kt`

- [ ] **Step 1: Write failing test using `runTest` virtual time**

```kotlin
package com.focusedreader.reader

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RsvpEngineTest {
    @Test fun `emits each token at wpm cadence`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RsvpEngine(dispatcher)
        val tokens = listOf("a", "b", "c")
        engine.start(tokens, startIndex = 0, wpm = 600) // 100ms/tick

        engine.index.test {
            assertEquals(0, awaitItem())
            advanceTimeBy(100); assertEquals(1, awaitItem())
            advanceTimeBy(100); assertEquals(2, awaitItem())
            advanceTimeBy(100); awaitComplete()
        }
    }

    @Test fun `pause stops emission`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RsvpEngine(dispatcher)
        engine.start(listOf("a", "b", "c"), 0, 600)
        engine.index.test {
            assertEquals(0, awaitItem())
            engine.pause()
            advanceTimeBy(500)
            expectNoEvents()
        }
    }

    @Test fun `setWpm changes next-tick delay`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RsvpEngine(dispatcher)
        engine.start(listOf("a","b","c"), 0, 600)
        engine.index.test {
            assertEquals(0, awaitItem())
            engine.setWpm(300) // 200ms/tick
            advanceTimeBy(199); expectNoEvents()
            advanceTimeBy(1); assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run test, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "*.RsvpEngineTest"`

- [ ] **Step 3: Implement**

```kotlin
package com.focusedreader.reader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RsvpEngine(private val dispatcher: CoroutineDispatcher) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var job: Job? = null
    private var tokens: List<String> = emptyList()
    private var current: Int = 0
    private val wpmFlow = MutableStateFlow(300)

    private val _index = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 64)
    val index: SharedFlow<Int> = _index.asSharedFlow()

    fun start(tokens: List<String>, startIndex: Int, wpm: Int) {
        this.tokens = tokens
        this.current = startIndex
        this.wpmFlow.value = wpm
        job?.cancel()
        job = scope.launch {
            while (current < tokens.size) {
                _index.emit(current)
                delay(Wpm.tickMillis(wpmFlow.value))
                current++
            }
        }
    }

    fun pause() { job?.cancel(); job = null }
    fun resume(wpm: Int) { start(tokens, current, wpm) }
    fun setWpm(wpm: Int) { wpmFlow.value = Wpm.clamp(wpm) }
    fun currentIndex(): Int = current
    fun shutdown() { scope.cancel() }
}
```

- [ ] **Step 4: Run test, verify PASS**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusedreader/reader/RsvpEngine.kt app/src/test/java/com/focusedreader/reader/RsvpEngineTest.kt
git commit -m "feat(reader): RsvpEngine tick loop with pause/resume/setWpm"
```

---

## Phase 2 — Theming + Navigation Skeleton

### Task 8: Theme + colors

**Files:**
- Create: `app/src/main/java/com/focusedreader/ui/theme/Color.kt`
- Create: `app/src/main/java/com/focusedreader/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/focusedreader/ui/theme/ReaderPalette.kt`

- [ ] **Step 1: Write `ReaderPalette.kt`**

```kotlin
package com.focusedreader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ReaderPalette(
    val background: Color,
    val word: Color,
    val orp: Color
) {
    companion object {
        val LightPure = ReaderPalette(Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFFFF0000))
        val LightSoft = ReaderPalette(Color(0xFFFAFAFA), Color(0xFF121212), Color(0xFFE53935))
        val DarkPure  = ReaderPalette(Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFFF0000))
        val DarkSoft  = ReaderPalette(Color(0xFF121212), Color(0xFFFAFAFA), Color(0xFFE53935))
    }
}

enum class ThemeMode { LIGHT, DARK }
enum class PaletteMode { PURE, SOFT }

fun palette(theme: ThemeMode, mode: PaletteMode): ReaderPalette = when (theme to mode) {
    ThemeMode.LIGHT to PaletteMode.PURE -> ReaderPalette.LightPure
    ThemeMode.LIGHT to PaletteMode.SOFT -> ReaderPalette.LightSoft
    ThemeMode.DARK  to PaletteMode.PURE -> ReaderPalette.DarkPure
    else -> ReaderPalette.DarkSoft
}
```

- [ ] **Step 2: Write `Color.kt`**

```kotlin
package com.focusedreader.ui.theme

import androidx.compose.ui.graphics.Color

internal val Md3Primary = Color(0xFF6750A4)
internal val Md3OnPrimary = Color(0xFFFFFFFF)
```

- [ ] **Step 3: Write `Theme.kt`**

```kotlin
package com.focusedreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalReaderPalette = staticCompositionLocalOf { ReaderPalette.DarkSoft }

@Composable
fun FocusedReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    readerPalette: ReaderPalette = if (darkTheme) ReaderPalette.DarkSoft else ReaderPalette.LightSoft,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalReaderPalette provides readerPalette) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
```

- [ ] **Step 4: Build, verify compiles**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusedreader/ui/theme/
git commit -m "feat(ui): theme + reader palettes (light/dark x pure/soft)"
```

---

### Task 9: Navigation graph + Home/Reader/Settings placeholders

**Files:**
- Create: `app/src/main/java/com/focusedreader/nav/Routes.kt`
- Create: `app/src/main/java/com/focusedreader/nav/NavGraph.kt`
- Create: `app/src/main/java/com/focusedreader/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/focusedreader/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/focusedreader/ui/reader/ReaderScreen.kt`
- Modify: `app/src/main/java/com/focusedreader/MainActivity.kt`

- [ ] **Step 1: Write `Routes.kt`**

```kotlin
package com.focusedreader.nav

object Routes {
    const val HOME = "home"
    const val READER = "reader"
    const val SETTINGS = "settings"
    const val TTS_CAL = "tts_calibration"
}
```

- [ ] **Step 2: Write placeholder screens**

`HomeScreen.kt`:
```kotlin
package com.focusedreader.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onRead: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Focused Reader")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRead) { Text("Read") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSettings) { Text("Settings") }
    }
}
```

`SettingsScreen.kt`:
```kotlin
package com.focusedreader.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Text("Settings (placeholder)", modifier = Modifier.fillMaxSize().padding(24.dp))
}
```

`ReaderScreen.kt`:
```kotlin
package com.focusedreader.ui.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ReaderScreen() {
    Text("Reader (placeholder)", modifier = Modifier.fillMaxSize())
}
```

- [ ] **Step 3: Write `NavGraph.kt`**

```kotlin
package com.focusedreader.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focusedreader.ui.home.HomeScreen
import com.focusedreader.ui.reader.ReaderScreen
import com.focusedreader.ui.settings.SettingsScreen

@Composable
fun FocusedReaderNavGraph(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onRead = { nav.navigate(Routes.READER) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.READER) { ReaderScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
```

- [ ] **Step 4: Update `MainActivity.kt`**

```kotlin
package com.focusedreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.focusedreader.nav.FocusedReaderNavGraph
import com.focusedreader.ui.theme.FocusedReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusedReaderTheme {
                FocusedReaderNavGraph()
            }
        }
    }
}
```

- [ ] **Step 5: Build, verify**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/focusedreader/
git commit -m "feat(ui): nav graph with Home/Reader/Settings placeholders"
```

---

## Phase 3 — Data Layer

### Task 10: Room session entity + DAO + database

**Files:**
- Create: `app/src/main/java/com/focusedreader/data/ImportSource.kt`
- Create: `app/src/main/java/com/focusedreader/data/Session.kt`
- Create: `app/src/main/java/com/focusedreader/data/SessionDao.kt`
- Create: `app/src/main/java/com/focusedreader/data/AppDatabase.kt`
- Create: `app/src/main/java/com/focusedreader/data/SessionRepository.kt`
- Create: `app/src/androidTest/java/com/focusedreader/data/SessionDaoTest.kt`

- [ ] **Step 1: Write entity + enum**

`ImportSource.kt`:
```kotlin
package com.focusedreader.data
enum class ImportSource { SHARE, A11Y, CLIPBOARD }
```

`Session.kt`:
```kotlin
package com.focusedreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_session")
data class Session(
    @PrimaryKey val id: Int = 0,
    val text: String,
    val position: Int,
    val source: ImportSource,
    val importedAt: Long
)
```

- [ ] **Step 2: Write DAO**

`SessionDao.kt`:
```kotlin
package com.focusedreader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: Session)

    @Query("UPDATE current_session SET position = :position WHERE id = 0")
    suspend fun updatePosition(position: Int)

    @Query("SELECT * FROM current_session WHERE id = 0")
    suspend fun get(): Session?

    @Query("SELECT * FROM current_session WHERE id = 0")
    fun observe(): Flow<Session?>

    @Query("DELETE FROM current_session")
    suspend fun clear()
}
```

- [ ] **Step 3: Write database with type converter for enum**

`AppDatabase.kt`:
```kotlin
package com.focusedreader.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class SourceConverter {
    @TypeConverter fun toString(s: ImportSource): String = s.name
    @TypeConverter fun fromString(s: String): ImportSource = ImportSource.valueOf(s)
}

@Database(entities = [Session::class], version = 1, exportSchema = false)
@TypeConverters(SourceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
}
```

- [ ] **Step 4: Write repository**

`SessionRepository.kt`:
```kotlin
package com.focusedreader.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(private val dao: SessionDao) {
    suspend fun import(text: String, source: ImportSource) {
        dao.upsert(Session(text = text, position = 0, source = source, importedAt = System.currentTimeMillis()))
    }
    suspend fun updatePosition(position: Int) = dao.updatePosition(position)
    suspend fun current(): Session? = dao.get()
    fun observe(): Flow<Session?> = dao.observe()
    suspend fun clear() = dao.clear()
}
```

- [ ] **Step 5: Write instrumented DAO test**

`SessionDaoTest.kt`:
```kotlin
package com.focusedreader.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SessionDao

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        dao = db.sessions()
    }
    @After fun tearDown() { db.close() }

    @Test fun upsert_then_get() = runBlocking {
        dao.upsert(Session(text = "hello", position = 0, source = ImportSource.SHARE, importedAt = 1L))
        val got = dao.get()!!
        assertEquals("hello", got.text)
        assertEquals(ImportSource.SHARE, got.source)
    }

    @Test fun upsert_replaces_existing() = runBlocking {
        dao.upsert(Session(text = "a", position = 0, source = ImportSource.SHARE, importedAt = 1L))
        dao.upsert(Session(text = "b", position = 5, source = ImportSource.A11Y, importedAt = 2L))
        val got = dao.get()!!
        assertEquals("b", got.text); assertEquals(5, got.position)
    }

    @Test fun updatePosition() = runBlocking {
        dao.upsert(Session(text = "x", position = 0, source = ImportSource.CLIPBOARD, importedAt = 1L))
        dao.updatePosition(42)
        assertEquals(42, dao.get()!!.position)
    }

    @Test fun clear_empties() = runBlocking {
        dao.upsert(Session(text = "x", position = 0, source = ImportSource.SHARE, importedAt = 1L))
        dao.clear()
        assertNull(dao.get())
    }
}
```

- [ ] **Step 6: Run instrumented test**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*.SessionDaoTest"`
Expected: PASS on an emulator/device. If no device, skip and verify by build only: `./gradlew :app:assembleDebugAndroidTest`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/focusedreader/data app/src/androidTest/java/com/focusedreader/data
git commit -m "feat(data): Room single-slot session entity, DAO, repository"
```

---

### Task 11: Settings DataStore

**Files:**
- Create: `app/src/main/java/com/focusedreader/data/SettingsRepository.kt`
- Modify: `app/src/main/java/com/focusedreader/FocusedReaderApp.kt`

- [ ] **Step 1: Write `SettingsRepository.kt`**

```kotlin
package com.focusedreader.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.focusedreader.ui.theme.PaletteMode
import com.focusedreader.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore("settings")

enum class HapticMode { OFF, PER_WORD, PER_PUNCTUATION }

data class Settings(
    val wpm: Int,
    val wpmStep: Int,
    val resumeDelaySec: Int,
    val faceDownPauseEnabled: Boolean,
    val hapticMode: HapticMode,
    val hapticIntensityPct: Int,
    val ttsEnabled: Boolean,
    val ttsWpmCap: Int,
    val themeMode: ThemeMode,
    val paletteMode: PaletteMode
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val ctx: Context) {
    private object Keys {
        val WPM = intPreferencesKey("wpm")
        val STEP = intPreferencesKey("wpm_step")
        val RESUME = intPreferencesKey("resume_delay")
        val FACE_DOWN = booleanPreferencesKey("face_down")
        val HAPTIC = stringPreferencesKey("haptic_mode")
        val HAPTIC_INT = intPreferencesKey("haptic_int")
        val TTS = booleanPreferencesKey("tts_enabled")
        val TTS_CAP = intPreferencesKey("tts_cap")
        val THEME = stringPreferencesKey("theme")
        val PALETTE = stringPreferencesKey("palette")
    }

    val settings: Flow<Settings> = ctx.settingsStore.data.map { p ->
        Settings(
            wpm = p[Keys.WPM] ?: 300,
            wpmStep = p[Keys.STEP] ?: 50,
            resumeDelaySec = p[Keys.RESUME] ?: 3,
            faceDownPauseEnabled = p[Keys.FACE_DOWN] ?: true,
            hapticMode = HapticMode.valueOf(p[Keys.HAPTIC] ?: HapticMode.OFF.name),
            hapticIntensityPct = p[Keys.HAPTIC_INT] ?: 10,
            ttsEnabled = p[Keys.TTS] ?: false,
            ttsWpmCap = p[Keys.TTS_CAP] ?: 400,
            themeMode = ThemeMode.valueOf(p[Keys.THEME] ?: ThemeMode.DARK.name),
            paletteMode = PaletteMode.valueOf(p[Keys.PALETTE] ?: PaletteMode.SOFT.name),
        )
    }

    suspend fun setWpm(v: Int) = edit { it[Keys.WPM] = v }
    suspend fun setStep(v: Int) = edit { it[Keys.STEP] = v }
    suspend fun setResumeDelay(v: Int) = edit { it[Keys.RESUME] = v }
    suspend fun setFaceDown(v: Boolean) = edit { it[Keys.FACE_DOWN] = v }
    suspend fun setHapticMode(v: HapticMode) = edit { it[Keys.HAPTIC] = v.name }
    suspend fun setHapticIntensity(v: Int) = edit { it[Keys.HAPTIC_INT] = v.coerceIn(0, 33) }
    suspend fun setTtsEnabled(v: Boolean) = edit { it[Keys.TTS] = v }
    suspend fun setTtsCap(v: Int) = edit { it[Keys.TTS_CAP] = v.coerceIn(100, 900) }
    suspend fun setTheme(v: ThemeMode) = edit { it[Keys.THEME] = v.name }
    suspend fun setPalette(v: PaletteMode) = edit { it[Keys.PALETTE] = v.name }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        ctx.settingsStore.edit(block)
    }
}
```

- [ ] **Step 2: Build verify**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/focusedreader/data/SettingsRepository.kt
git commit -m "feat(data): SettingsRepository on DataStore with all spec settings"
```

---

### Task 12: Hilt DI module

**Files:**
- Create: `app/src/main/java/com/focusedreader/di/AppModule.kt`

- [ ] **Step 1: Write module**

```kotlin
package com.focusedreader.di

import android.content.Context
import androidx.room.Room
import com.focusedreader.data.AppDatabase
import com.focusedreader.data.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "focused-reader.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessions()
}
```

- [ ] **Step 2: Build verify**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/focusedreader/di/AppModule.kt
git commit -m "feat(di): Hilt module for Room database + DAO"
```

---

## Phase 4 — Capture Pipeline

### Task 13: ImportTextUseCase + Share intent receiver

**Files:**
- Create: `app/src/main/java/com/focusedreader/capture/ImportTextUseCase.kt`
- Create: `app/src/main/java/com/focusedreader/capture/ShareReceiverActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write use case**

```kotlin
package com.focusedreader.capture

import com.focusedreader.data.ImportSource
import com.focusedreader.data.SessionRepository
import javax.inject.Inject

class ImportTextUseCase @Inject constructor(private val repo: SessionRepository) {
    sealed class Result {
        data object Empty : Result()
        data object Ok : Result()
    }
    suspend operator fun invoke(text: String?, source: ImportSource): Result {
        val cleaned = text?.trim().orEmpty()
        if (cleaned.isBlank()) return Result.Empty
        repo.import(cleaned, source)
        return Result.Ok
    }
}
```

- [ ] **Step 2: Write `ShareReceiverActivity.kt`**

```kotlin
package com.focusedreader.capture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.focusedreader.MainActivity
import com.focusedreader.data.ImportSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    @Inject lateinit var importer: ImportTextUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val result = runBlocking { importer(text, ImportSource.SHARE) }
        when (result) {
            ImportTextUseCase.Result.Empty -> Toast.makeText(this, "No text to read", Toast.LENGTH_SHORT).show()
            ImportTextUseCase.Result.Ok -> startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
}
```

- [ ] **Step 3: Update manifest — add inside `<application>`**

```xml
        <activity
            android:name=".capture.ShareReceiverActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Translucent.NoTitleBar"
            android:noHistory="true">
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
        </activity>
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusedreader/capture/ImportTextUseCase.kt app/src/main/java/com/focusedreader/capture/ShareReceiverActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat(capture): Share intent receiver + ImportTextUseCase"
```

---

### Task 14: Clipboard importer + Home screen wiring

**Files:**
- Create: `app/src/main/java/com/focusedreader/capture/ClipboardImporter.kt`
- Create: `app/src/main/java/com/focusedreader/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/focusedreader/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/focusedreader/nav/NavGraph.kt`

- [ ] **Step 1: Write `ClipboardImporter.kt`**

```kotlin
package com.focusedreader.capture

import android.content.ClipboardManager
import android.content.Context
import com.focusedreader.data.ImportSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardImporter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val importer: ImportTextUseCase
) {
    suspend fun importFromClipboard(): ImportTextUseCase.Result {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        return importer(text, ImportSource.CLIPBOARD)
    }
}
```

- [ ] **Step 2: Write `HomeViewModel.kt`**

```kotlin
package com.focusedreader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.capture.ClipboardImporter
import com.focusedreader.capture.ImportTextUseCase
import com.focusedreader.data.Session
import com.focusedreader.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SessionRepository,
    private val clipboard: ClipboardImporter
) : ViewModel() {
    val session: StateFlow<Session?> = repo.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun importFromClipboard(onResult: (ImportTextUseCase.Result) -> Unit) {
        viewModelScope.launch { onResult(clipboard.importFromClipboard()) }
    }
}
```

- [ ] **Step 3: Update `HomeScreen.kt`**

```kotlin
package com.focusedreader.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.capture.ImportTextUseCase
import android.widget.Toast

@Composable
fun HomeScreen(
    onRead: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val session by vm.session.collectAsState()
    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Focused Reader", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        session?.let {
            Text("Last import: ${it.source.name}", style = MaterialTheme.typography.bodyMedium)
            Text(it.text.take(80) + if (it.text.length > 80) "…" else "", style = MaterialTheme.typography.bodySmall)
            Text("Position: ${it.position} / words", style = MaterialTheme.typography.bodySmall)
        } ?: Text("No imported text yet", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRead, enabled = session != null) { Text("Read") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            vm.importFromClipboard { result ->
                val msg = when (result) {
                    ImportTextUseCase.Result.Empty -> "Clipboard is empty"
                    ImportTextUseCase.Result.Ok -> "Imported from clipboard"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        }) { Text("Paste from clipboard") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSettings) { Text("Settings") }
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusedreader/capture/ClipboardImporter.kt app/src/main/java/com/focusedreader/ui/home/
git commit -m "feat(capture): clipboard importer + Home wired to session/import"
```

---

### Task 15: Accessibility Service + Quick Settings tile

**Files:**
- Create: `app/src/main/java/com/focusedreader/capture/FocusedReaderA11yService.kt`
- Create: `app/src/main/java/com/focusedreader/capture/QuickCaptureTileService.kt`
- Create: `app/src/main/res/xml/a11y_service_config.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write a11y config**

`app/src/main/res/xml/a11y_service_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:description="@string/a11y_description"
    android:notificationTimeout="100" />
```

Add string to `strings.xml`:
```xml
<string name="a11y_description">Captures text from the foreground app on demand.</string>
```

- [ ] **Step 2: Write `FocusedReaderA11yService.kt`**

```kotlin
package com.focusedreader.capture

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focusedreader.data.ImportSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusedReaderA11yService : AccessibilityService() {

    @Inject lateinit var importer: ImportTextUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var capturePending = false

    companion object {
        const val ACTION_CAPTURE = "com.focusedreader.action.CAPTURE_NOW"
        @Volatile var instance: FocusedReaderA11yService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun requestCapture() {
        val text = collectText(rootInActiveWindow)
        scope.launch { importer(text, ImportSource.A11Y) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun collectText(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val out = StringBuilder()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            val t = n.text?.toString()?.trim().orEmpty()
            val d = n.contentDescription?.toString()?.trim().orEmpty()
            if (t.isNotBlank()) { out.append(t); out.append(' ') }
            else if (d.isNotBlank()) { out.append(d); out.append(' ') }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        return out.toString().trim()
    }
}
```

- [ ] **Step 3: Write `QuickCaptureTileService.kt`**

```kotlin
package com.focusedreader.capture

import android.service.quicksettings.TileService
import android.widget.Toast

class QuickCaptureTileService : TileService() {
    override fun onClick() {
        val svc = FocusedReaderA11yService.instance
        if (svc == null) {
            Toast.makeText(this, "Enable Focused Reader accessibility service first", Toast.LENGTH_LONG).show()
        } else {
            svc.requestCapture()
            Toast.makeText(this, "Captured", Toast.LENGTH_SHORT).show()
        }
    }
}
```

- [ ] **Step 4: Update manifest — add inside `<application>`**

```xml
        <service
            android:name=".capture.FocusedReaderA11yService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data android:name="android.accessibilityservice"
                       android:resource="@xml/a11y_service_config" />
        </service>

        <service
            android:name=".capture.QuickCaptureTileService"
            android:label="Capture text"
            android:icon="@android:drawable/ic_menu_edit"
            android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.service.quicksettings.action.QS_TILE" />
            </intent-filter>
        </service>
```

- [ ] **Step 5: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/focusedreader/capture/FocusedReaderA11yService.kt app/src/main/java/com/focusedreader/capture/QuickCaptureTileService.kt app/src/main/res/xml/a11y_service_config.xml app/src/main/res/values/strings.xml app/src/main/AndroidManifest.xml
git commit -m "feat(capture): A11y service + Quick Settings tile for on-demand capture"
```

---

## Phase 5 — Reader UI (ORP rendering)

### Task 16: ORP word display composable

**Files:**
- Create: `app/src/main/java/com/focusedreader/ui/reader/OrpWord.kt`

- [ ] **Step 1: Implement composable that renders ORP-split word with pivot at fixed anchor**

```kotlin
package com.focusedreader.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.focusedreader.reader.OrpCalculator

@Composable
fun OrpWord(
    word: String,
    wordColor: Color,
    orpColor: Color,
    fontSize: TextUnit,
    pivotAnchorFraction: Float = 0.38f,
    modifier: Modifier = Modifier
) {
    if (word.isEmpty()) return
    val split = remember(word) { OrpCalculator.split(word) }
    val style = LocalTextStyle.current.copy(fontSize = fontSize, fontWeight = FontWeight.Medium)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val anchorX = totalWidth * pivotAnchorFraction

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(anchorX))
                Text(split.pivot.toString(), color = orpColor, style = style)
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(anchorX))
                Text(
                    text = split.left,
                    color = wordColor,
                    style = style,
                    modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(x = -measureWidth(split.left, style, density = density), y = 0) }
                )
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(anchorX))
                Text(
                    text = split.right,
                    color = wordColor,
                    style = style,
                    modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(x = measureWidth(split.pivot.toString(), style, density = density), y = 0) }
                )
            }
        }
    }
}

// Note: measureWidth implemented in next step (Paragraph-based measurement).
private fun measureWidth(text: String, style: TextStyle, density: androidx.compose.ui.unit.Density): Int = 0 // TODO replaced below
```

**Note for engineer:** the above is a sketch — the offset approach is fragile. Replace in Step 2 with a measured layout using `Layout` composable.

- [ ] **Step 2: Replace with `Layout`-based composable for precise anchor placement**

Replace entire `OrpWord.kt`:

```kotlin
package com.focusedreader.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import com.focusedreader.reader.OrpCalculator

@Composable
fun OrpWord(
    word: String,
    wordColor: Color,
    orpColor: Color,
    fontSize: TextUnit,
    pivotAnchorFraction: Float = 0.38f,
    modifier: Modifier = Modifier
) {
    if (word.isEmpty()) return
    val split = remember(word) { OrpCalculator.split(word) }
    val baseStyle: TextStyle = LocalTextStyle.current.copy(fontSize = fontSize, fontWeight = FontWeight.Medium)

    Box(modifier = modifier.fillMaxSize()) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Text(split.left, color = wordColor, style = baseStyle, maxLines = 1)
                Text(split.pivot.toString(), color = orpColor, style = baseStyle, maxLines = 1)
                Text(split.right, color = wordColor, style = baseStyle, maxLines = 1)
            }
        ) { measurables, constraints ->
            val unbounded = constraints.copy(minWidth = 0, maxWidth = Int.MAX_VALUE / 4)
            val leftPlaceable = measurables[0].measure(unbounded)
            val pivotPlaceable = measurables[1].measure(unbounded)
            val rightPlaceable = measurables[2].measure(unbounded)

            val totalWidth = constraints.maxWidth
            val totalHeight = constraints.maxHeight
            val anchorX = (totalWidth * pivotAnchorFraction).toInt()
            val centerY = (totalHeight - pivotPlaceable.height) / 2

            val pivotX = anchorX - pivotPlaceable.width / 2
            val leftX = pivotX - leftPlaceable.width
            val rightX = pivotX + pivotPlaceable.width

            layout(totalWidth, totalHeight) {
                leftPlaceable.place(leftX, centerY)
                pivotPlaceable.place(pivotX, centerY)
                rightPlaceable.place(rightX, centerY)
            }
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/focusedreader/ui/reader/OrpWord.kt
git commit -m "feat(ui): OrpWord composable with fixed pivot anchor via Layout"
```

---

### Task 17: ReaderViewModel + ReaderScreen end-to-end

**Files:**
- Create: `app/src/main/java/com/focusedreader/ui/reader/ReaderViewModel.kt`
- Modify: `app/src/main/java/com/focusedreader/ui/reader/ReaderScreen.kt`

- [ ] **Step 1: Write `ReaderViewModel.kt`**

```kotlin
package com.focusedreader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.data.SessionRepository
import com.focusedreader.data.SettingsRepository
import com.focusedreader.reader.ReaderState
import com.focusedreader.reader.RsvpEngine
import com.focusedreader.reader.WordTokenizer
import com.focusedreader.reader.Wpm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val engine = RsvpEngine(Dispatchers.Default)
    private val _state = MutableStateFlow<ReaderState>(ReaderState.Idle)
    val state: StateFlow<ReaderState> = _state

    private var tokens: List<String> = emptyList()
    private var saveCounter = 0

    init {
        viewModelScope.launch {
            val session = sessions.current() ?: return@launch
            val s = settings.settings.first()
            tokens = WordTokenizer.tokenize(session.text)
            val startIdx = session.position.coerceIn(0, tokens.size)
            val maxWpm = if (s.ttsEnabled) s.ttsWpmCap else Wpm.DEFAULT_MAX
            val wpm = Wpm.clamp(s.wpm, max = maxWpm)
            _state.value = ReaderState.Reading(tokens, startIdx, wpm)
            startEngine(startIdx, wpm)
        }
        viewModelScope.launch {
            engine.index.collect { idx ->
                _state.update { cur ->
                    when (cur) {
                        is ReaderState.Reading -> cur.copy(index = idx)
                        else -> cur
                    }
                }
                saveCounter++
                if (saveCounter % 5 == 0) sessions.updatePosition(idx)
            }
        }
    }

    private fun startEngine(idx: Int, wpm: Int) {
        engine.start(tokens, idx, wpm)
    }

    fun togglePause() {
        viewModelScope.launch {
            when (val cur = _state.value) {
                is ReaderState.Reading -> {
                    engine.pause()
                    sessions.updatePosition(cur.index)
                    _state.value = ReaderState.Paused(cur.tokens, cur.index, cur.wpm)
                }
                is ReaderState.Paused -> {
                    val delaySec = settings.settings.first().resumeDelaySec
                    _state.value = ReaderState.Resuming(cur.tokens, cur.index, cur.wpm, delaySec)
                    countdownThenResume(delaySec, cur)
                }
                is ReaderState.Resuming -> {
                    _state.value = ReaderState.Paused(cur.tokens, cur.index, cur.wpm)
                }
                ReaderState.Idle -> Unit
            }
        }
    }

    private fun countdownThenResume(seconds: Int, base: ReaderState.Paused) {
        viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                _state.value = ReaderState.Resuming(base.tokens, base.index, base.wpm, left)
                kotlinx.coroutines.delay(1000)
                if (_state.value !is ReaderState.Resuming) return@launch
                left--
            }
            _state.value = ReaderState.Reading(base.tokens, base.index, base.wpm)
            startEngine(base.index, base.wpm)
        }
    }

    fun bumpWpm(delta: Int) {
        viewModelScope.launch {
            val s = settings.settings.first()
            val maxWpm = if (s.ttsEnabled) s.ttsWpmCap else Wpm.DEFAULT_MAX
            val newWpm = Wpm.clamp(currentWpm() + delta, max = maxWpm)
            engine.setWpm(newWpm)
            settings.setWpm(newWpm)
            _state.update { cur ->
                when (cur) {
                    is ReaderState.Reading -> cur.copy(wpm = newWpm)
                    is ReaderState.Paused -> cur.copy(wpm = newWpm)
                    is ReaderState.Resuming -> cur.copy(wpm = newWpm)
                    ReaderState.Idle -> cur
                }
            }
        }
    }

    private fun currentWpm(): Int = when (val s = _state.value) {
        is ReaderState.Reading -> s.wpm
        is ReaderState.Paused -> s.wpm
        is ReaderState.Resuming -> s.wpm
        ReaderState.Idle -> 300
    }

    fun stop() {
        engine.pause()
        _state.value = ReaderState.Idle
    }

    override fun onCleared() {
        engine.shutdown()
        super.onCleared()
    }
}
```

- [ ] **Step 2: Replace `ReaderScreen.kt`**

```kotlin
package com.focusedreader.ui.reader

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.reader.ReaderState
import com.focusedreader.ui.theme.LocalReaderPalette

@Composable
fun ReaderScreen(
    onExit: () -> Unit,
    onSettings: () -> Unit,
    vm: ReaderViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val palette = LocalReaderPalette.current
    val ctx = LocalContext.current

    DisposableEffect(Unit) {
        val activity = ctx as? ComponentActivity
        val prior = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = prior ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable { vm.togglePause() }
    ) {
        when (val s = state) {
            ReaderState.Idle -> Text("No session", color = palette.word, modifier = Modifier.align(Alignment.Center))
            is ReaderState.Reading -> OrpWord(
                word = s.tokens.getOrNull(s.index) ?: "",
                wordColor = palette.word, orpColor = palette.orp, fontSize = 96.sp
            )
            is ReaderState.Paused -> PauseOverlay(
                wpm = s.wpm,
                onResume = { vm.togglePause() },
                onStop = { vm.stop(); onExit() },
                onSettings = onSettings,
                palette = palette
            )
            is ReaderState.Resuming -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Resuming in ${s.secondsLeft}…", color = palette.word)
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    wpm: Int,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSettings: () -> Unit,
    palette: com.focusedreader.ui.theme.ReaderPalette
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Paused", color = palette.word)
        Text("$wpm WPM", color = palette.word)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onResume) { Text("Resume") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onStop) { Text("Stop") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSettings) { Text("Settings") }
    }
}
```

- [ ] **Step 3: Update `NavGraph.kt` to pass callbacks**

Replace the `composable(Routes.READER)` block with:
```kotlin
        composable(Routes.READER) {
            com.focusedreader.ui.reader.ReaderScreen(
                onExit = { nav.popBackStack() },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
```

- [ ] **Step 4: Wire volume keys in `MainActivity.kt`** — replace file:

```kotlin
package com.focusedreader

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusedreader.data.SettingsRepository
import com.focusedreader.nav.FocusedReaderNavGraph
import com.focusedreader.ui.theme.FocusedReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository
    val keyEvents = MutableSharedFlow<Int>(extraBufferCapacity = 16) // KEYCODE_VOLUME_UP / DOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusedReaderTheme {
                FocusedReaderNavGraph()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                keyEvents.tryEmit(keyCode)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
```

- [ ] **Step 5: Subscribe to volume keys in `ReaderScreen`** — add inside Reader composable above `Box`:

```kotlin
    LaunchedEffect(Unit) {
        val activity = ctx as? com.focusedreader.MainActivity ?: return@LaunchedEffect
        val step = 50 // will be replaced by settings in Task 19
        activity.keyEvents.collect { code ->
            when (code) {
                android.view.KeyEvent.KEYCODE_VOLUME_UP -> vm.bumpWpm(+step)
                android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> vm.bumpWpm(-step)
            }
        }
    }
```

- [ ] **Step 6: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/focusedreader/ui/reader/ ReaderViewModel.kt app/src/main/java/com/focusedreader/nav/NavGraph.kt app/src/main/java/com/focusedreader/MainActivity.kt
git commit -m "feat(reader): ReaderViewModel + ReaderScreen end-to-end with volume controls"
```

---

## Phase 6 — Sensors, Haptics, TTS

### Task 18: Orientation monitor (face-up/down pause)

**Files:**
- Create: `app/src/main/java/com/focusedreader/reader/OrientationMonitor.kt`
- Modify: `app/src/main/java/com/focusedreader/ui/reader/ReaderScreen.kt`
- Modify: `app/src/main/java/com/focusedreader/ui/reader/ReaderViewModel.kt`

- [ ] **Step 1: Write `OrientationMonitor.kt`**

```kotlin
package com.focusedreader.reader

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class FaceOrientation { UP, DOWN, UNKNOWN }

@Singleton
class OrientationMonitor @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun orientationEvents(debounceMs: Long = 500): Flow<FaceOrientation> = callbackFlow {
        val mgr = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = mgr.getDefaultSensor(Sensor.TYPE_GRAVITY) // gravity widely available; orient detection
        if (sensor == null) { close(); return@callbackFlow }
        var last = FaceOrientation.UNKNOWN
        var lastEmitMs = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val z = event.values[2]
                val orient = when {
                    z > 7f -> FaceOrientation.UP
                    z < -7f -> FaceOrientation.DOWN
                    else -> return
                }
                val now = System.currentTimeMillis()
                if (orient != last && now - lastEmitMs > debounceMs) {
                    last = orient
                    lastEmitMs = now
                    trySend(orient)
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        mgr.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { mgr.unregisterListener(listener) }
    }
}
```

**Note:** Using `TYPE_GRAVITY` since `TYPE_DEVICE_ORIENTATION` is API 30 named but actual constant `Sensor.TYPE_DEVICE_PRIVATE_BASE` mappings vary; gravity Z-axis is the universally reliable signal. Spec § 5.2 stated TYPE_DEVICE_ORIENTATION but functional equivalence retained.

- [ ] **Step 2: Inject + observe in `ReaderViewModel`** — modify constructor and add init block

Add to constructor:
```kotlin
    private val orientation: com.focusedreader.reader.OrientationMonitor
```

Add to `init`:
```kotlin
        viewModelScope.launch {
            val s = settings.settings.first()
            if (!s.faceDownPauseEnabled) return@launch
            orientation.orientationEvents().collect { face ->
                when (face) {
                    com.focusedreader.reader.FaceOrientation.DOWN -> {
                        if (_state.value is ReaderState.Reading) togglePause()
                    }
                    com.focusedreader.reader.FaceOrientation.UP -> {
                        if (_state.value is ReaderState.Paused) togglePause()
                    }
                    else -> Unit
                }
            }
        }
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/focusedreader/reader/OrientationMonitor.kt app/src/main/java/com/focusedreader/ui/reader/ReaderViewModel.kt
git commit -m "feat(reader): face-up/down sensor pause via OrientationMonitor"
```

---

### Task 19: Haptic controller + wire into RSVP ticks

**Files:**
- Create: `app/src/main/java/com/focusedreader/reader/HapticController.kt`
- Modify: `app/src/main/java/com/focusedreader/ui/reader/ReaderViewModel.kt`

- [ ] **Step 1: Write `HapticController.kt`**

```kotlin
package com.focusedreader.reader

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.focusedreader.data.HapticMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticController @Inject constructor(@ApplicationContext ctx: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val punctuation = setOf('.', '!', '?', ',', ';', ':')

    fun tick(word: String, mode: HapticMode, intensityPct: Int) {
        if (mode == HapticMode.OFF || intensityPct <= 0) return
        if (mode == HapticMode.PER_PUNCTUATION && word.lastOrNull() !in punctuation) return
        val amplitude = ((intensityPct / 100.0) * 255).toInt().coerceIn(1, 255)
        vibrator.vibrate(VibrationEffect.createOneShot(15, amplitude))
    }
}
```

- [ ] **Step 2: Wire in `ReaderViewModel`** — inject `HapticController` + `SettingsRepository.settings` snapshot

In the existing `engine.index.collect` block, replace body with:
```kotlin
            engine.index.collect { idx ->
                _state.update { cur ->
                    when (cur) {
                        is ReaderState.Reading -> cur.copy(index = idx)
                        else -> cur
                    }
                }
                val s = settings.settings.first()
                val word = tokens.getOrNull(idx) ?: ""
                haptic.tick(word, s.hapticMode, s.hapticIntensityPct)
                saveCounter++
                if (saveCounter % 5 == 0) sessions.updatePosition(idx)
            }
```

Add `haptic: HapticController` to constructor.

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/focusedreader/reader/HapticController.kt app/src/main/java/com/focusedreader/ui/reader/ReaderViewModel.kt
git commit -m "feat(reader): haptic ticks per-word or per-punctuation"
```

---

### Task 20: TTS controller + wire into ticks

**Files:**
- Create: `app/src/main/java/com/focusedreader/reader/TtsController.kt`
- Modify: `app/src/main/java/com/focusedreader/ui/reader/ReaderViewModel.kt`

- [ ] **Step 1: Write `TtsController.kt`**

```kotlin
package com.focusedreader.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TtsController @Inject constructor(@ApplicationContext private val ctx: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    suspend fun init(): Boolean = suspendCancellableCoroutine { cont ->
        val instance = TextToSpeech(ctx) { status ->
            ready = status == TextToSpeech.SUCCESS
            cont.resume(ready)
        }
        tts = instance
        cont.invokeOnCancellation { instance.stop(); instance.shutdown() }
    }

    fun speak(word: String) {
        if (!ready) return
        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, word)
    }

    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null; ready = false }
}
```

- [ ] **Step 2: Inject + use in `ReaderViewModel`**

Add `tts: TtsController` to constructor. In `init`, after settings loaded:
```kotlin
            if (s.ttsEnabled) tts.init()
```

In tick `collect` block, after `haptic.tick(...)`:
```kotlin
                if (s.ttsEnabled) tts.speak(word)
```

In `onCleared()`:
```kotlin
        tts.shutdown()
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/focusedreader/reader/TtsController.kt app/src/main/java/com/focusedreader/ui/reader/ReaderViewModel.kt
git commit -m "feat(reader): TTS speaks each word in sync with visual tick"
```

---

## Phase 7 — Settings UI

### Task 21: Full Settings screen

**Files:**
- Modify: `app/src/main/java/com/focusedreader/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/focusedreader/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Write `SettingsViewModel.kt`**

```kotlin
package com.focusedreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.data.HapticMode
import com.focusedreader.data.Settings
import com.focusedreader.data.SettingsRepository
import com.focusedreader.ui.theme.PaletteMode
import com.focusedreader.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    val settings: StateFlow<Settings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWpm(v: Int) = viewModelScope.launch { repo.setWpm(v) }
    fun setStep(v: Int) = viewModelScope.launch { repo.setStep(v) }
    fun setResume(v: Int) = viewModelScope.launch { repo.setResumeDelay(v) }
    fun setFaceDown(v: Boolean) = viewModelScope.launch { repo.setFaceDown(v) }
    fun setHaptic(m: HapticMode) = viewModelScope.launch { repo.setHapticMode(m) }
    fun setHapticIntensity(v: Int) = viewModelScope.launch { repo.setHapticIntensity(v) }
    fun setTts(v: Boolean) = viewModelScope.launch { repo.setTtsEnabled(v) }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { repo.setTheme(t) }
    fun setPalette(p: PaletteMode) = viewModelScope.launch { repo.setPalette(p) }
}
```

- [ ] **Step 2: Replace `SettingsScreen.kt`**

```kotlin
package com.focusedreader.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.data.HapticMode
import com.focusedreader.ui.theme.PaletteMode
import com.focusedreader.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onCalibrateTts: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val current = s ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Section("Speed") {
            SliderRow("WPM step (${current.wpmStep})", current.wpmStep.toFloat(), 10f..100f, 9) { vm.setStep(it.toInt()) }
            SliderRow("WPM (${current.wpm})", current.wpm.toFloat(), 100f..900f, 16) { vm.setWpm(it.toInt()) }
        }
        Section("Pause / Resume") {
            SliderRow("Resume delay (${current.resumeDelaySec}s)", current.resumeDelaySec.toFloat(), 0f..10f, 10) { vm.setResume(it.toInt()) }
            SwitchRow("Face-down pause", current.faceDownPauseEnabled) { vm.setFaceDown(it) }
        }
        Section("Haptic") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mode: ")
                HapticMode.values().forEach { m ->
                    FilterChip(selected = current.hapticMode == m, onClick = { vm.setHaptic(m) }, label = { Text(m.name) }, modifier = Modifier.padding(end = 4.dp))
                }
            }
            SliderRow("Intensity (${current.hapticIntensityPct}%)", current.hapticIntensityPct.toFloat(), 0f..33f, 33) { vm.setHapticIntensity(it.toInt()) }
        }
        Section("TTS") {
            SwitchRow("Enable TTS", current.ttsEnabled) { vm.setTts(it) }
            Text("WPM cap: ${current.ttsWpmCap}")
            Button(onClick = onCalibrateTts) { Text("Calibrate") }
        }
        Section("Theme") {
            Row {
                ThemeMode.values().forEach { t ->
                    FilterChip(selected = current.themeMode == t, onClick = { vm.setTheme(t) }, label = { Text(t.name) }, modifier = Modifier.padding(end = 4.dp))
                }
            }
            Row {
                PaletteMode.values().forEach { p ->
                    FilterChip(selected = current.paletteMode == p, onClick = { vm.setPalette(p) }, label = { Text(p.name) }, modifier = Modifier.padding(end = 4.dp))
                }
            }
        }
        Section("Capture") {
            Button(onClick = {
                ctx.startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Open Accessibility Settings") }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    Column(content = content)
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Text(label)
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
```

- [ ] **Step 3: Update `NavGraph.kt`**

Replace `composable(Routes.SETTINGS)` block:
```kotlin
        composable(Routes.SETTINGS) {
            com.focusedreader.ui.settings.SettingsScreen(
                onCalibrateTts = { nav.navigate(Routes.TTS_CAL) }
            )
        }
```

Add stub for TTS calibration (built in next task):
```kotlin
        composable(Routes.TTS_CAL) {
            com.focusedreader.ui.settings.TtsCalibrationScreen(onDone = { nav.popBackStack() })
        }
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: may fail referring to `TtsCalibrationScreen` — that's OK, comment out for now or add empty placeholder file.

Create stub `app/src/main/java/com/focusedreader/ui/settings/TtsCalibrationScreen.kt`:
```kotlin
package com.focusedreader.ui.settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
@Composable
fun TtsCalibrationScreen(onDone: () -> Unit) { Text("TBD") }
```

Run build again.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusedreader/ui/settings/ app/src/main/java/com/focusedreader/nav/NavGraph.kt
git commit -m "feat(ui): full Settings screen wired to SettingsRepository"
```

---

### Task 22: TTS calibration wizard

**Files:**
- Modify: `app/src/main/java/com/focusedreader/ui/settings/TtsCalibrationScreen.kt`
- Create: `app/src/main/java/com/focusedreader/ui/settings/TtsCalibrationViewModel.kt`

- [ ] **Step 1: Write `TtsCalibrationViewModel.kt`**

```kotlin
package com.focusedreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.data.SettingsRepository
import com.focusedreader.reader.TtsController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TtsCalibrationViewModel @Inject constructor(
    private val tts: TtsController,
    private val settings: SettingsRepository
) : ViewModel() {

    data class State(val low: Int = 100, val high: Int = 900, val current: Int = 500, val done: Boolean = false)

    private val testSentence = "The quick brown fox jumps over the lazy dog while reading at calibration speed"
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun init() = viewModelScope.launch { tts.init() }

    fun speakCurrent() = viewModelScope.launch {
        val words = testSentence.split(" ")
        val delayMs = 60_000L / _state.value.current
        for (w in words) { tts.speak(w); kotlinx.coroutines.delay(delayMs) }
    }

    fun answer(understandable: Boolean) {
        val s = _state.value
        val newState = if (understandable) {
            s.copy(low = s.current, current = (s.current + s.high) / 2)
        } else {
            s.copy(high = s.current, current = (s.current + s.low) / 2)
        }
        if (newState.high - newState.low <= 25) {
            _state.value = newState.copy(done = true, current = newState.low)
            viewModelScope.launch { settings.setTtsCap(newState.low) }
        } else {
            _state.value = newState
        }
    }
}
```

- [ ] **Step 2: Replace `TtsCalibrationScreen.kt`**

```kotlin
package com.focusedreader.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TtsCalibrationScreen(onDone: () -> Unit, vm: TtsCalibrationViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.init() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.done) {
            Text("Calibration complete: cap = ${state.current} WPM", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone) { Text("Done") }
        } else {
            Text("Testing ${state.current} WPM", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.speakCurrent() }) { Text("Play sample") }
            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = { vm.answer(true) }) { Text("Understandable") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { vm.answer(false) }) { Text("Too fast") }
            }
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/focusedreader/ui/settings/TtsCalibration*
git commit -m "feat(settings): TTS calibration wizard with binary-search WPM cap"
```

---

## Phase 8 — Polish

### Task 23: Wire WPM step from settings into Reader volume keys

**Files:**
- Modify: `app/src/main/java/com/focusedreader/ui/reader/ReaderScreen.kt`
- Modify: `app/src/main/java/com/focusedreader/ui/reader/ReaderViewModel.kt`

- [ ] **Step 1: Expose `wpmStep` from `ReaderViewModel`**

Add to `ReaderViewModel`:
```kotlin
    val wpmStep: StateFlow<Int> = kotlinx.coroutines.flow.MutableStateFlow(50).also { flow ->
        viewModelScope.launch {
            settings.settings.collect { flow.value = it.wpmStep }
        }
    }
```

- [ ] **Step 2: Use in `ReaderScreen` volume key block**

Replace step-related lines in the LaunchedEffect:
```kotlin
    val step by vm.wpmStep.collectAsState()
    LaunchedEffect(Unit) {
        val activity = ctx as? com.focusedreader.MainActivity ?: return@LaunchedEffect
        activity.keyEvents.collect { code ->
            when (code) {
                android.view.KeyEvent.KEYCODE_VOLUME_UP -> vm.bumpWpm(+step)
                android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> vm.bumpWpm(-step)
            }
        }
    }
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/focusedreader/ui/reader/
git commit -m "feat(reader): volume key step comes from settings"
```

---

### Task 24: Compose UI test — Home renders, navigates to Reader

**Files:**
- Create: `app/src/androidTest/java/com/focusedreader/ui/home/HomeScreenTest.kt`

- [ ] **Step 1: Write test**

```kotlin
package com.focusedreader.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun shows_buttons_and_routes() {
        var readClicked = false
        rule.setContent {
            HomeScreen(onRead = { readClicked = true }, onSettings = {})
        }
        rule.onNodeWithText("Read").assertIsDisplayed().performClick()
        assert(!readClicked) // Read disabled when no session — verifies disabled state
        rule.onNodeWithText("Settings").assertIsDisplayed()
        rule.onNodeWithText("Paste from clipboard").assertIsDisplayed()
    }
}
```

**Note:** This test uses `HomeScreen` directly without Hilt VM injection — the composable signature accepts callbacks but `vm: HomeViewModel = hiltViewModel()` will fail in test. Engineer should either: (a) refactor `HomeScreen` to accept a `session: Session?` and `onPaste` callback (preferred — pure UI), or (b) use `HiltAndroidRule`. Pick (a):

Replace `HomeScreen.kt` signature & body to accept state instead of VM:
```kotlin
@Composable
fun HomeScreenContent(
    session: com.focusedreader.data.Session?,
    onRead: () -> Unit,
    onSettings: () -> Unit,
    onPasteFromClipboard: () -> Unit
) { /* body using parameters */ }

@Composable
fun HomeScreen(onRead: () -> Unit, onSettings: () -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val session by vm.session.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    HomeScreenContent(
        session = session,
        onRead = onRead,
        onSettings = onSettings,
        onPasteFromClipboard = {
            vm.importFromClipboard { result ->
                val msg = when (result) {
                    com.focusedreader.capture.ImportTextUseCase.Result.Empty -> "Clipboard is empty"
                    com.focusedreader.capture.ImportTextUseCase.Result.Ok -> "Imported from clipboard"
                }
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )
}
```

Update test to call `HomeScreenContent`.

- [ ] **Step 2: Run test (requires emulator)**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*.HomeScreenTest"`

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest app/src/main/java/com/focusedreader/ui/home/HomeScreen.kt
git commit -m "test(ui): HomeScreen renders buttons; refactor for testability"
```

---

### Task 25: Final manual smoke test + README

**Files:**
- Create: `README.md`

- [ ] **Step 1: Write README**

```markdown
# Focused Reader (Android)

POC RSVP speed-reader. Highlight text in another app → Share → Focused Reader.
Volume keys change WPM. Tap or face-down pauses; tap or face-up resumes.

## Build

Requires Android SDK. Set `local.properties`:
```
sdk.dir=/home/blentz/Android
```

Then: `./gradlew :app:assembleDebug`

Install: `./gradlew :app:installDebug`

## Capture paths

1. **Share** — from any app's Share sheet → "Focused Reader".
2. **Accessibility** — enable in System Settings → Accessibility → Focused Reader. Trigger via Quick Settings tile "Capture text".
3. **Clipboard** — copy text in any app, return to Focused Reader Home, tap "Paste from clipboard".

## Design

See `docs/superpowers/specs/2026-05-16-focused-reader-android-design.md`.
```

- [ ] **Step 2: Manual smoke (engineer's local device)**

1. Install on device: `./gradlew :app:installDebug`
2. Share text from Chrome → app opens, displays first word.
3. Volume Up several times → words speed up.
4. Tap screen → pauses with menu.
5. Flip face-down → pauses (if was reading).
6. Flip face-up → countdown then resume.
7. Open Settings → adjust theme/haptic/TTS toggle.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: README with build + capture instructions"
```

---

## Self-Review Notes (resolved inline)

- Spec § 3.2 A11y on-demand trigger → Task 15 covers via Quick Settings tile (notification action deferred — single trigger sufficient per spec § 17 open item).
- Spec § 4.4 font sizing "session-constant" → not yet measured at runtime; current `ReaderScreen` uses fixed 96sp. **Known gap** — engineer should add a Task 26 to measure widest token via `Paragraph` and feed `fontSize` into `OrpWord`. Out of POC scope but flagged.
- Spec § 5.5 startup with no session → `HomeScreen` shows "No imported text yet" and disables Read button. Acceptable.
- Spec § 13 error handling → covered ad-hoc; no dedicated task. Toasts inline.
- TYPE_DEVICE_ORIENTATION vs TYPE_GRAVITY: Task 18 notes substitution; both achieve face-up/down detection identically for POC.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-16-focused-reader-android.md`. Two execution options:

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks.
2. **Inline Execution** — executing-plans, batch with checkpoints.
