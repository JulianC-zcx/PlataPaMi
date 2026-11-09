package com.platapami.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun Cargando(vm: AppViewModel) {
    if (vm.cargando) LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp), color = C.Teal, trackColor = C.Cyan)
    else Spacer(Modifier.height(3.dp))
    if (vm.sinConexion) {
        Text(
            "Sin conexión: mostrando los datos guardados en tu celular",
            Modifier.fillMaxWidth().background(Color(0xFFFFF1DC)).padding(horizontal = 28.dp, vertical = 6.dp),
            fontSize = 12.sp, textAlign = TextAlign.Center
        )
    }
    vm.errorCarga?.let {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp)) {
            Aviso(it)
            Text("Reintentar", color = C.Teal, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.cargarMes() }.padding(vertical = 6.dp))
        }
    }
}

private fun colorGastos(gastos: List<Mov>) = gastos.mapIndexed { i, m -> m.monto to GastoColors[i % GastoColors.size] }

// ───────────── 1. HOME ─────────────
@Composable
fun HomeScreen(nav: NavController, vm: AppViewModel) {
    val gastos = vm.movs.filter { !it.entrada }
    var detalle by remember { mutableStateOf<Mov?>(null) }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan)) {
            Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vm.mes.rango(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("HOLA ${(vm.usuario?.primerNombre ?: "").uppercase()}", fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Este es tu presupuesto del mes:", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(24.dp)).background(C.Cream)
                            .clickable { nav.navigate("movimientos/todos") }.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) { SearchIcon(); Spacer(Modifier.width(14.dp)); Text("BUSCAR") }
                }
                Donut(colorGastos(gastos), vm.saldo, Modifier.size(160.dp), grosor = 46f)
            }
            BellBtn(vm.sinLeer, Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp), tam = 38.dp) { nav.navigate("notificaciones") }
        }
        Cargando(vm)
        SelectorMes(vm)
        Column(Modifier.padding(horizontal = 28.dp)) {
            Text("Presupuesto: ${vm.saldo.cop()} COP", fontSize = 16.sp, color = if (vm.saldo < 0) C.Pink else Color.Black)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Btn("Entradas", C.Green) { nav.navigate("movimientos/entradas") }
                Btn("Gastos", C.Pink) { nav.navigate("movimientos/salidas") }
            }
            Spacer(Modifier.height(14.dp))
            Btn("Análisis de tus finanzas", C.Teal, Modifier.fillMaxWidth()) { nav.navigate("grafico") }
            Spacer(Modifier.height(14.dp))
            Btn("Extracto financiero", C.Indigo, Modifier.fillMaxWidth()) { nav.navigate("extracto") }
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
            if (gastos.isEmpty() && !vm.cargando) {
                Text(
                    "Aún no tienes gastos en ${vm.mes.largo()}.\nToca el botón + para registrar el primero.",
                    textAlign = TextAlign.Center, color = C.Grey, fontSize = 14.sp, modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    gastos.take(2).forEach { MovCard(it, Modifier.weight(1f), mostrarTipo = true) { detalle = it } }
                    if (gastos.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    detalle?.let { MovDetalleDialog(it, nav, vm) { detalle = null } }
}

// ───────────── 2. GRÁFICO ─────────────
@Composable
fun GraficoScreen(nav: NavController, vm: AppViewModel) {
    val gastos = vm.movs.filter { !it.entrada }
    var desglose by remember { mutableStateOf(false) }
    // El 100 % es lo que entró en el mes; si gastaste más de lo que entró, el 100 % son los gastos.
    val base = maxOf(vm.entradas, vm.gastos)

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Cabecera("GRÁFICO", nav, vm)
        Cargando(vm)
        SelectorMes(vm)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Donut(colorGastos(gastos), vm.saldo, Modifier.size(300.dp), grosor = 130f)
            if (vm.movs.isEmpty() && !vm.cargando) Text("Sin movimientos", color = C.Grey, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (base > 0) {
            Text(
                "Saldo ${pct(vm.saldo.coerceAtLeast(0), base)} · Gastos ${pct(vm.gastos, base)}",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 14.sp
            )
            if (vm.entradas > 0 && vm.gastos * 100 >= vm.entradas * 80) {
                Text(
                    if (vm.gastos > vm.entradas) "⚠️ Te pasaste del presupuesto del mes" else "⚠️ Ya gastaste el 80% o más de tus entradas",
                    Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center, fontSize = 13.sp, color = C.Pink, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)).background(C.Cyan).padding(24.dp)
        ) {
            gastos.forEachIndexed { i, m ->
                FilaTotal(m.nombre.uppercase(), m.monto.cop() + if (desglose) "  (${pct(m.monto, base)})" else "", Color.Black, GastoColors[i % GastoColors.size])
                HorizontalDivider(color = Color.Black)
            }
            FilaTotal("SALDO", vm.saldo.cop() + if (desglose) "  (${pct(vm.saldo.coerceAtLeast(0), base)})" else "", C.Teal, C.Orange)
            if (desglose) {
                HorizontalDivider(color = Color.Black)
                FilaTotal("TOTAL ENTRADAS", vm.entradas.cop(), C.Green)
                HorizontalDivider(color = Color.Black)
                FilaTotal("TOTAL GASTOS", vm.gastos.cop(), C.Pink)
            }
            Spacer(Modifier.height(20.dp))
            Btn(if (desglose) "Ocultar" else "Desglosar", C.Sky, Modifier.fillMaxWidth()) { desglose = !desglose }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilaTotal(l: String, r: String, colorL: Color, punto: Color? = null) =
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (punto != null) { Box(Modifier.size(10.dp).clip(CircleShape).background(punto)); Spacer(Modifier.width(8.dp)) }
            Text(l, fontWeight = FontWeight.Bold, color = colorL, fontSize = 14.sp)
        }
        Text(r, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
