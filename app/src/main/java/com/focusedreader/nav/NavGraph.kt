package com.focusedreader.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focusedreader.ui.home.HomeScreen
import com.focusedreader.ui.reader.ReaderScreen
import com.focusedreader.ui.settings.SettingsScreen
import com.focusedreader.ui.settings.TtsCalibrationScreen

@Composable
fun FocusedReaderNavGraph(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onRead = { nav.navigate(Routes.READER) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.READER) {
            com.focusedreader.ui.reader.ReaderScreen(
                onExit = { nav.popBackStack() },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onCalibrateTts = { nav.navigate(Routes.TTS_CAL) }
            )
        }
        composable(Routes.TTS_CAL) {
            TtsCalibrationScreen(onDone = { nav.popBackStack() })
        }
    }
}
