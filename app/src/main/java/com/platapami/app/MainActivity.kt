package com.platapami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlataTheme { App() } }
    }
}

@Composable
fun App() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    NavHost(nav, startDestination = "registro") {
        composable("registro") { RegistroScreen(nav, vm) }
        composable("home") { HomeScreen(nav, vm) }
        composable("grafico") { GraficoScreen(nav, vm) }
        composable("movimientos/{filtro}", listOf(navArgument("filtro") { type = NavType.StringType })) {
            MovimientosScreen(nav, vm, it.arguments?.getString("filtro") ?: "todos")
        }
    }
}
