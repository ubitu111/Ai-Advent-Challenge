package ru.mirtomsk.macosapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.mirtomsk.shared.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AI Advent Challenge"
    ) {
        App()
    }
}

