package com.platapami.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ───────────── 1. HOME ─────────────
@Composable
fun HomeScreen(nav: NavController, vm: AppViewModel) {
    val gastos = vm.movs.filter { !it.entrada }
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("AGO 01 - AGO 31 / 2026", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(16.dp))
                Text("HOLA ${vm.usuario.uppercase()}", fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Text("Este es tu presupuesto del mes:", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(24.dp)).background(C.Cream)
                        .clickable { nav.navigate("movimientos/todos") }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { SearchIcon(); Spacer(Modifier.width(14.dp)); Text("BUSCAR") }
            }
            Donut(
                gastos.mapIndexed { i, m -> m.monto to GastoColors[i % GastoColors.size] }, vm.saldo,
                Modifier.size(170.dp), grosor = 48f
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 28.dp)) {
            Text("Presupuesto: ${vm.saldo.cop()} COP", fontSize = 16.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Btn("Entradas", C.Green) { nav.navigate("movimientos/entradas") }
                Btn("Gastos", C.Pink) { nav.navigate("movimientos/salidas") }
            }
            Spacer(Modifier.height(14.dp))
            Btn("Análisis de tus finanzas", C.Teal, Modifier.fillMaxWidth()) { nav.navigate("grafico") }
            Spacer(Modifier.height(14.dp))
            Btn("Extracto financiero", C.Indigo, Modifier.fillMaxWidth()) { nav.navigate("movimientos/todos") }
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(32.dp)).background(C.Orange.copy(alpha = .45f)),
                Alignment.CenterStart
            ) {
                Text("🪙", fontSize = 90.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 28.dp))
                Box(
                    Modifier.padding(start = 20.dp).clip(RoundedCornerShape(30.dp)).background(C.Sky)
                        .clickable { nav.navigate("movimientos/todos") }.padding(horizontal = 18.dp, vertical = 14.dp)
                ) { Text("VER REGISTRO", color = Color.White, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                gastos.take(2).forEach { MovCard(it, Modifier.weight(1f), mostrarTipo = true) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ───────────── 2. GRÁFICO ─────────────
@Composable
fun GraficoScreen(nav: NavController, vm: AppViewModel) {
    val gastos = vm.movs.filter { !it.entrada }
    var desglose by remember { mutableStateOf(false) }
    val total = vm.entradas
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(horizontal = 24.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BackBtn { nav.popBackStack() }
            Text("GRÁFICO", fontWeight = FontWeight.Bold, fontSize = 28.sp)
            BellBtn()
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Donut(gastos.mapIndexed { i, m -> m.monto to GastoColors[i % GastoColors.size] }, vm.saldo, Modifier.size(300.dp), grosor = 130f)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Saldo ${pct(vm.saldo, total)} · Gastos ${pct(vm.gastos, total)}",
            Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)).background(C.Cyan).padding(24.dp)
        ) {
            gastos.forEach {
                FilaTotal(it.nombre.uppercase(), it.monto.cop() + if (desglose) "  (${pct(it.monto, total)})" else "", Color.Black)
                HorizontalDivider(color = Color.Black)
            }
            FilaTotal("SALDO", vm.saldo.cop() + if (desglose) "  (${pct(vm.saldo, total)})" else "", C.Teal)
            Spacer(Modifier.height(20.dp))
            Btn(if (desglose) "Ocultar" else "Desglosar", C.Sky, Modifier.fillMaxWidth()) { desglose = !desglose }
        }
    }
}

@Composable
private fun FilaTotal(l: String, r: String, colorL: Color) =
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(l, fontWeight = FontWeight.Bold, color = colorL, fontSize = 14.sp)
        Text(r, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

// ───────────── 3. MOVIMIENTOS ─────────────
@Composable
fun MovimientosScreen(nav: NavController, vm: AppViewModel, filtroInicial: String) {
    var filtro by remember { mutableStateOf(filtroInicial) }
    var query by remember { mutableStateOf("") }
    val lista = vm.movs.filter {
        (filtro == "todos" || (filtro == "entradas") == it.entrada) &&
            (query.isBlank() || it.nombre.contains(query, true) || it.detalle.contains(query, true))
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(horizontal = 24.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackBtn { nav.popBackStack() }
            Spacer(Modifier.width(12.dp))
            TextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Buscar...") }, leadingIcon = { SearchIcon() },
                shape = RoundedCornerShape(30.dp), modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = C.Cream, unfocusedContainerColor = C.Cream,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(Modifier.width(12.dp))
            BellBtn()
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Chip("Entradas", C.LightGreen, filtro == "entradas") { filtro = "entradas" }
            Chip("Salidas", C.Red, filtro == "salidas") { filtro = "salidas" }
            Chip("Todos", C.Sky, filtro == "todos", Color.White) { filtro = "todos" }
        }
        LazyVerticalGrid(
            GridCells.Fixed(2), contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)
        ) { items(lista, key = { it.id }) { MovCard(it) } }
    }
}

@Composable
private fun Chip(t: String, c: Color, sel: Boolean, tc: Color = Color.Black, onClick: () -> Unit) =
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(c)
            .border(if (sel) BorderStroke(2.dp, Color.Black) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp)
    ) { Text(t, color = tc, fontSize = 18.sp, fontWeight = FontWeight.Light) }

// ───────────── 4. REGISTRO DE USUARIO ─────────────
@Composable
fun RegistroScreen(nav: NavController, vm: AppViewModel) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = C.Cream, unfocusedContainerColor = C.Cream,
        focusedBorderColor = C.Teal, unfocusedBorderColor = Color.Transparent
    )
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(32.dp, 56.dp, 32.dp, 40.dp)) {
            Text("PlataPaMi", fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Text("Crea tu cuenta y organiza tu presupuesto 💸", fontSize = 16.sp)
        }
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("REGISTRO", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true,
                shape = RoundedCornerShape(20.dp), colors = colors, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(correo, { correo = it }, label = { Text("Correo electrónico") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(20.dp), colors = colors, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(clave, { clave = it }, label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(20.dp), colors = colors, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = C.Pink, fontSize = 13.sp) }
            Spacer(Modifier.height(6.dp))
            Btn("Crear cuenta", C.Sky, Modifier.fillMaxWidth()) {
                error = when {
                    nombre.isBlank() -> "Escribe tu nombre"
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "Correo no válido"
                    clave.length < 6 -> "La contraseña debe tener mínimo 6 caracteres"
                    else -> null
                }
                if (error == null) {
                    vm.usuario = nombre.trim()
                    nav.navigate("home") { popUpTo("registro") { inclusive = true } }
                }
            }
            Text(
                "¿Ya tienes cuenta? Ingresa", color = C.Teal, modifier = Modifier.align(Alignment.CenterHorizontally)
                    .clickable { nav.navigate("home") { popUpTo("registro") { inclusive = true } } }
            )
        }
    }
}
