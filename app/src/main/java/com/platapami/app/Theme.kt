package com.platapami.app

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Paleta de los mockups (cian / verde / rojo) */
object C {
    val Cyan = Color(0xFF9CF6FF)
    val Orange = Color(0xFFF4B86C)
    val Green = Color(0xFF00C060)
    val LightGreen = Color(0xFF7ED957)
    val Pink = Color(0xFFC93567)
    val Teal = Color(0xFF0099B8)
    val Indigo = Color(0xFF536DFE)
    val Sky = Color(0xFF3AB5FF)
    val Red = Color(0xFFFF5757)
    val Cream = Color(0xFFFFFEF8)
    val Grey = Color(0xFF6B7478)
}

val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private fun Typography.conFuente(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

@Composable
fun PlataTheme(content: @Composable () -> Unit) {
    MaterialTheme(typography = Typography().conFuente(Poppins)) {
        // Sin Surface, Text() usaría la fuente del sistema: así toda la app queda en Poppins.
        CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Poppins)) {
            content()
        }
    }
}
