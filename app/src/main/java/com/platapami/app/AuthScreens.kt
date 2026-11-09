package com.platapami.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private val EMAIL = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

/** Ver / Ocultar contraseña */
@Composable
private fun VerClave(ver: Boolean, onToggle: () -> Unit) =
    Text(if (ver) "Ocultar" else "Ver", color = C.Teal, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onToggle).padding(12.dp))

@Composable
private fun CabeceraMarca(subtitulo: String) =
    Column(Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(32.dp, 56.dp, 32.dp, 40.dp)) {
        Text("PlataPaMi", fontWeight = FontWeight.Bold, fontSize = 34.sp)
        Text(subtitulo, fontSize = 16.sp)
    }

// ───────────── INICIO DE SESIÓN ─────────────
@Composable
fun LoginScreen(nav: NavController, vm: AppViewModel) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var ver by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        CabeceraMarca("Bienvenido de vuelta 👋")
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Campo(correo, { correo = it }, "Correo electrónico", teclado = KeyboardOptions(keyboardType = KeyboardType.Email))
            Campo(
                clave, { clave = it }, "Contraseña", teclado = KeyboardOptions(keyboardType = KeyboardType.Password),
                transformacion = if (ver) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = { VerClave(ver) { ver = !ver } }
            )
            error?.let { Aviso(it) }
            Text(
                "¿Olvidaste tu contraseña?", color = C.Teal, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.End).clickable { nav.navigate("olvide") }
            )
            Btn("Ingresar", C.Sky, Modifier.fillMaxWidth(), loading = cargando) {
                error = when {
                    !EMAIL.matches(correo.trim()) -> "Escribe un correo válido"
                    clave.isEmpty() -> "Escribe tu contraseña"
                    else -> null
                }
                if (error == null) {
                    cargando = true
                    vm.login(correo, clave) { err -> cargando = false; error = err }
                }
            }
            Text(
                "¿No tienes cuenta? Regístrate", color = C.Teal,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { nav.navigate("registro") }.padding(8.dp)
            )
        }
    }
}

// ───────────── RESTABLECER CONTRASEÑA (enlace al correo) ─────────────
@Composable
fun OlvideScreen(nav: NavController, vm: AppViewModel) {
    var correo by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var enviado by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(horizontal = 24.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackBtn { nav.popBackStack() }
            Spacer(Modifier.width(16.dp))
            Text("RESTABLECER", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("¿Olvidaste tu contraseña?", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Escribe el correo con el que te registraste y te enviaremos un enlace para crear una contraseña nueva.", fontSize = 14.sp)
            Campo(correo, { correo = it }, "Correo electrónico", teclado = KeyboardOptions(keyboardType = KeyboardType.Email))
            error?.let { Aviso(it) }
            enviado?.let {
                Aviso(it, ok = true)
                Text("Revisa también la carpeta de spam. El enlace funciona una sola vez y vence en 1 hora.", fontSize = 12.sp, color = C.Grey)
            }
            Btn("Enviar enlace", C.Sky, Modifier.fillMaxWidth(), loading = cargando) {
                if (!EMAIL.matches(correo.trim())) { error = "Escribe un correo válido"; return@Btn }
                error = null; enviado = null; cargando = true
                vm.olvideContrasena(correo) { msg, err -> cargando = false; enviado = msg; error = err }
            }
            Text(
                "Volver a iniciar sesión", color = C.Teal,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { nav.popBackStack() }.padding(8.dp)
            )
        }
    }
}

// ───────────── REGISTRO ─────────────
@Composable
fun RegistroScreen(nav: NavController, vm: AppViewModel) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var tipoDoc by rememberSaveable { mutableStateOf("CC") }
    var documento by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var clave2 by rememberSaveable { mutableStateOf("") }
    var ver by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val acepto = vm.terminos

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        CabeceraMarca("Crea tu cuenta y organiza tu presupuesto 💸")
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("REGISTRO", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Campo(nombre, { nombre = it }, "Nombre completo")

            Text("Documento de identidad", fontSize = 13.sp, color = C.Grey)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("CC", "CE", "TI", "PA").forEach { t ->
                    Text(
                        t, fontSize = 15.sp, fontWeight = if (tipoDoc == t) FontWeight.Bold else FontWeight.Light,
                        color = if (tipoDoc == t) Color.White else Color.Black,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (tipoDoc == t) C.Sky else C.Cream)
                            .clickable { tipoDoc = t }.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }
            Campo(
                documento, { documento = it.filter { c -> c.isLetterOrDigit() }.take(15) }, "Número de documento",
                teclado = KeyboardOptions(keyboardType = if (tipoDoc == "PA") KeyboardType.Text else KeyboardType.Number)
            )
            Text("Será la contraseña de tu extracto en PDF.", fontSize = 12.sp, color = C.Grey)

            Campo(correo, { correo = it }, "Correo electrónico", teclado = KeyboardOptions(keyboardType = KeyboardType.Email))
            Campo(
                clave, { clave = it }, "Contraseña", teclado = KeyboardOptions(keyboardType = KeyboardType.Password),
                transformacion = if (ver) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = { VerClave(ver) { ver = !ver } }
            )
            Campo(
                clave2, { clave2 = it }, "Repite la contraseña", teclado = KeyboardOptions(keyboardType = KeyboardType.Password),
                transformacion = if (ver) VisualTransformation.None else PasswordVisualTransformation()
            )
            Text("Mínimo 8 caracteres, con letras y números.", fontSize = 12.sp, color = C.Grey)

            // ── Términos y condiciones + Habeas Data (obligatorio aceptar) ──
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Cream)
                    .border(2.dp, if (acepto == true) C.Green else if (acepto == false) C.Pink else Color(0xFFE3EAEC), RoundedCornerShape(20.dp))
                    .clickable { nav.navigate("terminos") }.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Términos y Condiciones y Habeas Data", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    when (acepto) {
                        true -> "✓ Los leíste y aceptaste"
                        false -> "✗ No los aceptaste: sin aceptar no puedes registrarte"
                        null -> "Toca aquí para leerlos y aceptar o rechazar"
                    },
                    fontSize = 13.sp, color = when (acepto) { true -> C.Green; false -> C.Pink; null -> C.Teal }
                )
            }

            error?.let { Aviso(it) }
            Spacer(Modifier.height(4.dp))
            Btn("Crear cuenta", C.Sky, Modifier.fillMaxWidth(), enabled = acepto == true, loading = cargando) {
                val doc = documento.trim()
                error = when {
                    acepto != true -> "Debes aceptar los Términos y Condiciones y Habeas Data"
                    nombre.trim().length < 2 -> "Escribe tu nombre completo"
                    doc.length < 5 || (tipoDoc != "PA" && !doc.all { it.isDigit() }) -> "Número de documento no válido"
                    !EMAIL.matches(correo.trim()) -> "Correo no válido"
                    clave.length < 8 || clave.none { it.isLetter() } || clave.none { it.isDigit() } ->
                        "La contraseña debe tener mínimo 8 caracteres, con letras y números"
                    clave != clave2 -> "Las contraseñas no coinciden"
                    else -> null
                }
                if (error == null) {
                    cargando = true
                    vm.registrar(nombre, tipoDoc, doc, correo, clave) { err, _ -> cargando = false; error = err }
                }
            }
            if (acepto != true) Text("Para crear la cuenta primero acepta los términos.", fontSize = 12.sp, color = C.Grey, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(
                "¿Ya tienes cuenta? Ingresa", color = C.Teal,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { nav.popBackStack() }.padding(8.dp)
            )
        }
    }
}

// ───────────── TÉRMINOS Y CONDICIONES + HABEAS DATA ─────────────
@Composable
fun TerminosScreen(nav: NavController, vm: AppViewModel, soloLectura: Boolean) {
    val scroll = rememberScrollState()
    val leido by remember { derivedStateOf { scroll.maxValue == 0 || scroll.value >= scroll.maxValue - 40 } }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(horizontal = 24.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackBtn { nav.popBackStack() }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("TÉRMINOS Y CONDICIONES", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("y política de Habeas Data · v${Legal.VERSION}", fontSize = 13.sp)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Legal.secciones.forEach {
                Text(it.titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(it.texto, fontSize = 13.sp, lineHeight = 20.sp)
            }
            Text("Ley 1581 de 2012 · Habeas Data", fontSize = 11.sp, color = C.Grey)
        }
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (soloLectura) {
                Btn("Cerrar", C.Sky, Modifier.fillMaxWidth()) { nav.popBackStack() }
            } else {
                if (!leido) Text("Desliza hasta el final para poder aceptar.", fontSize = 12.sp, color = C.Grey, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Btn("No acepto", C.Pink, Modifier.weight(1f)) { vm.terminos = false; nav.popBackStack() }
                    Btn("Acepto", C.Green, Modifier.weight(1f), enabled = leido) { vm.terminos = true; nav.popBackStack() }
                }
            }
        }
    }
}
