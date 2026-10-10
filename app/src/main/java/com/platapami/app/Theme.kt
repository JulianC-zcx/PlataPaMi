package com.platapami.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

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
}

val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

@Composable
fun PlataTheme(content: @Composable () -> Unit) {
    val b = Typography()
    val t = Typography(
        bodyLarge = b.bodyLarge.copy(fontFamily = Poppins),
        bodyMedium = b.bodyMedium.copy(fontFamily = Poppins),
        bodySmall = b.bodySmall.copy(fontFamily = Poppins),
        labelLarge = b.labelLarge.copy(fontFamily = Poppins),
        labelMedium = b.labelMedium.copy(fontFamily = Poppins),
        titleMedium = b.titleMedium.copy(fontFamily = Poppins),
        titleLarge = b.titleLarge.copy(fontFamily = Poppins)
    )
    MaterialTheme(typography = t, content = content)
}
