package com.platapami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlataTheme { App(vm) } }
    }

    /** Al volver a la app: refresca los datos y, si cambió el mes, pasa solo al mes nuevo. */
    override fun onResume() {
        super.onResume()
        vm.onResume()
    }
}

/** Pantallas sin sesión */
private val RUTAS_AUTH = setOf("login", "registro", "olvide", "terminos")
/** Pantallas que muestran el menú inferior */
private val RUTAS_CON_BARRA = setOf("home", "grafico", "extracto", "perfil")

@Composable
fun App(vm: AppViewModel) {
    val nav = rememberNavController()
    val ruta = nav.currentBackStackEntryAsState().value?.destination?.route
    val haySesion = vm.usuario != null

    // Entrar / salir de sesión cambia de pantalla solo (registro, login, cierre de sesión o sesión vencida)
    LaunchedEffect(haySesion) {
        val actual = nav.currentDestination?.route
        if (haySesion && actual in RUTAS_AUTH) nav.navigate("home") { popUpTo(0) }
        if (!haySesion && actual != null && actual !in RUTAS_AUTH) nav.navigate("login") { popUpTo(0) }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = { if (haySesion && ruta in RUTAS_CON_BARRA) BarraInferior(nav, ruta) }
    ) { inner ->
        Box(Modifier.fillMaxSize().background(Color.White).padding(inner)) {
            NavHost(nav, startDestination = if (haySesion) "home" else "login") {
                composable("login") { LoginScreen(nav, vm) }
                composable("registro") { RegistroScreen(nav, vm) }
                composable("olvide") { OlvideScreen(nav, vm) }
                composable("terminos") { TerminosScreen(nav, vm, soloLectura = false) }
                composable("terminos/ver") { TerminosScreen(nav, vm, soloLectura = true) }

                composable("home") { HomeScreen(nav, vm) }
                composable("grafico") { GraficoScreen(nav, vm) }
                composable("extracto") { ExtractoScreen(nav, vm) }
                composable("perfil") { PerfilScreen(nav, vm) }
                composable("notificaciones") { NotificacionesScreen(nav, vm) }
                composable("movimientos/{filtro}", listOf(navArgument("filtro") { type = NavType.StringType })) {
                    MovimientosScreen(nav, vm, it.arguments?.getString("filtro") ?: "todos")
                }
                composable("movimiento") { MovimientoFormScreen(nav, vm, null) }
                composable("movimiento/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    MovimientoFormScreen(nav, vm, it.arguments?.getLong("id"))
                }
            }
        }
    }
}
