package com.platapami.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.text.NumberFormat
import java.util.Locale

data class Mov(val id: Int, val nombre: String, val detalle: String, val monto: Long, val entrada: Boolean, val emoji: String)

class AppViewModel : ViewModel() {
    var usuario by mutableStateOf("Esteban")
    val movs = mutableStateListOf(
        Mov(1, "Lamborghini", "Lamborghini", 100_000_000, false, "🏎️"),
        Mov(2, "Helado Crepes", "Crepes", 2_500_000, false, "🍦"),
        Mov(3, "Helado Popsy", "Popsy", 2_500_000, false, "🍧"),
        Mov(4, "Salario", "JC Industries", 8_500_000, true, "💵"),
        Mov(5, "Baloto", "Baloto", 12_000_000_000, true, "🎰"),
        Mov(6, "Café", "Juan Valdez", 2_000_000, false, "☕")
    )
    val entradas get() = movs.filter { it.entrada }.sumOf { it.monto }
    val gastos get() = movs.filter { !it.entrada }.sumOf { it.monto }
    val saldo get() = entradas - gastos
}

fun Long.cop(): String = "$ " + NumberFormat.getInstance(Locale("es", "CO")).format(this)
fun pct(parte: Long, total: Long): String =
    if (total <= 0) "0%" else String.format(Locale("es", "CO"), "%.1f%%", parte * 100.0 / total)
