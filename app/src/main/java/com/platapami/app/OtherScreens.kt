package com.platapami.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ───────────── 5. EXTRACTO FINANCIERO ─────────────
@Composable
fun ExtractoScreen(nav: NavController, vm: AppViewModel) {
    var enviando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val u = vm.usuario

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Cabecera("EXTRACTO", nav, vm)
        Cargando(vm)
        SelectorMes(vm)
        Column(Modifier.padding(horizontal = 28.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Resumen de ${vm.mes.largo()}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Resumen("Entradas", vm.entradas.cop(), Color(0xFFE8FAEF), C.Green, Modifier.weight(1f))
                Resumen("Gastos", vm.gastos.cop(), Color(0xFFFFE9EF), C.Pink, Modifier.weight(1f))
            }
            Resumen("Saldo del mes", vm.saldo.cop(), Color(0xFFE0FBFF), C.Teal, Modifier.fillMaxWidth())

            Text("Movimientos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (vm.movs.isEmpty() && !vm.cargando) {
                Text("No hay movimientos en este mes. El extracto saldrá con valores en cero.", color = C.Grey, fontSize = 13.sp)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Cyan.copy(alpha = .35f)).padding(horizontal = 16.dp, vertical = 4.dp)) {
                vm.movs.sortedBy { it.fecha }.forEachIndexed { i, m ->
                    if (i > 0) HorizontalDivider(color = Color.White)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text(m.fecha.fechaCorta(), fontSize = 11.sp, color = C.Grey)
                        }
                        Text((if (m.entrada) "+ " else "− ") + m.monto.cop(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (m.entrada) C.Green else C.Pink)
                    }
                }
            }

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Cream).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🔒 PDF protegido", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Te lo enviamos a ${u?.correo ?: "tu correo"}. Para abrirlo usa como contraseña tu número de documento" +
                        (u?.documentoMask?.takeIf { it.isNotBlank() }?.let { " (${u.tipoDocumento} $it)" } ?: "") + ".",
                    fontSize = 13.sp
                )
            }
            Btn("Enviar extracto a mi correo", C.Indigo, Modifier.fillMaxWidth(), loading = enviando) {
                enviando = true
                vm.enviarExtracto(vm.mes) { ok, msg -> enviando = false; resultado = ok to msg }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    resultado?.let { (ok, msg) ->
        AlertDialog(
            onDismissRequest = { resultado = null },
            containerColor = Color.White,
            title = { Text(if (ok) "¡Extracto enviado! 📬" else "No se pudo enviar", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { resultado = null }) { Text("Listo", color = C.Teal, fontWeight = FontWeight.Bold) } }
        )
    }
}

@Composable
private fun Resumen(titulo: String, valor: String, fondo: Color, color: Color, modifier: Modifier) =
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(fondo).padding(16.dp)) {
        Text(titulo, fontSize = 12.sp, color = color)
        Text(valor, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
    }

// ───────────── 6. NOTIFICACIONES (campana) ─────────────
@Composable
fun NotificacionesScreen(nav: NavController, vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.cargarNotifs() }
    // Se marcan como leídas al salir, para que el punto rojo siga visible mientras las lees
    DisposableEffect(Unit) { onDispose { vm.marcarLeidas() } }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(horizontal = 24.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackBtn { nav.popBackStack() }
            Spacer(Modifier.width(16.dp))
            Text("NOTIFICACIONES", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        if (vm.notifs.isEmpty()) {
            Text(
                "Aquí verás tus alertas de gasto (al llegar al 80 % de tus entradas) y los avisos de tu extracto mensual.",
                Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = C.Grey
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.notifs.forEach { n ->
                val (emoji, fondo) = when (n.tipo) {
                    "alerta100" -> "🚨" to Color(0xFFFFE9EF)
                    "alerta80" -> "⚠️" to Color(0xFFFFF1DC)
                    "extracto" -> "📄" to Color(0xFFE0FBFF)
                    else -> "ℹ️" to C.Cream
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(fondo).padding(16.dp)) {
                    Text(emoji, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(n.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(n.mensaje, fontSize = 13.sp)
                        Text(n.creado.take(16).replace('-', '/'), fontSize = 11.sp, color = C.Grey)
                    }
                    if (!n.leida) Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(C.Pink))
                }
            }
        }
    }
}

// ───────────── 7. PERFIL ─────────────
@Composable
fun PerfilScreen(nav: NavController, vm: AppViewModel) {
    var confirmar by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    val u = vm.usuario
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Cabecera("PERFIL", nav, vm, conAtras = false)
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Cream).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Dato("Nombre", u?.nombre ?: "")
                Dato("Correo", u?.correo ?: "")
                Dato("Documento", "${u?.tipoDocumento ?: ""} ${u?.documentoMask ?: ""}")
            }
            Btn("Términos y Habeas Data", C.Teal, Modifier.fillMaxWidth()) { nav.navigate("terminos/ver") }
            Btn("Cerrar sesión", C.Pink, Modifier.fillMaxWidth()) { confirmar = true }
            Text(
                "Eliminar mi cuenta y mis datos", color = C.Grey, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { borrar = true }.padding(8.dp)
            )
            Text("PlataPaMi v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = C.Grey, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            containerColor = Color.White,
            title = { Text("¿Cerrar sesión?", fontWeight = FontWeight.Bold) },
            text = { Text("Tus datos quedan guardados en tu cuenta; podrás volver a entrar cuando quieras.") },
            confirmButton = { TextButton(onClick = { confirmar = false; vm.cerrarSesion() }) { Text("Cerrar sesión", color = C.Pink, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }
    if (borrar) EliminarCuentaDialog(vm) { borrar = false }
}

/** Derecho de supresión de la Ley 1581: borra la cuenta, los movimientos y los comprobantes (pide la contraseña). */
@Composable
private fun EliminarCuentaDialog(vm: AppViewModel, onClose: () -> Unit) {
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!cargando) onClose() },
        containerColor = Color.White,
        title = { Text("¿Eliminar tu cuenta?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Se borrarán para siempre tu cuenta, tus entradas, gastos y comprobantes. No se puede deshacer. Escribe tu contraseña para confirmar.", fontSize = 14.sp)
                Campo(
                    clave, { clave = it }, "Contraseña", transformacion = PasswordVisualTransformation(),
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Password), error = error != null
                )
                error?.let { Aviso(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !cargando && clave.isNotEmpty(), onClick = {
                cargando = true; error = null
                vm.eliminarCuenta(clave) { err -> cargando = false; if (err != null) error = err }
            }) { Text("Eliminar", color = C.Pink, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(enabled = !cargando, onClick = onClose) { Text("Cancelar") } }
    )
}

@Composable
private fun Dato(titulo: String, valor: String) = Column {
    Text(titulo, fontSize = 12.sp, color = C.Grey)
    Text(valor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
}
