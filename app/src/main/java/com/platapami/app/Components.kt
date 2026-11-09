package com.platapami.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

val HeaderShape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp)

// ───────────── Botones ─────────────
@Composable
fun Btn(
    text: String, color: Color, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit
) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (enabled) color else color.copy(alpha = .4f))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(26.dp), color = Color.White, strokeWidth = 3.dp)
        else Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
    }
}

@Composable
fun CircleBtn(modifier: Modifier = Modifier, tam: Dp = 52.dp, onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box(modifier.size(tam).clip(CircleShape).background(Color.White).clickable(onClick = onClick), Alignment.Center) { content() }
}

@Composable
fun BackBtn(onClick: () -> Unit) = CircleBtn(onClick = onClick) { Icon(Icons.Default.ArrowBack, "Atrás") }

@Composable
fun BellBtn(pendientes: Int = 0, modifier: Modifier = Modifier, tam: Dp = 52.dp, onClick: () -> Unit = {}) = CircleBtn(modifier, tam, onClick) {
    Box {
        Icon(Icons.Default.Notifications, "Notificaciones")
        if (pendientes > 0) Box(Modifier.size(10.dp).clip(CircleShape).background(C.Pink).align(Alignment.TopEnd))
    }
}

@Composable
fun SearchIcon() = Icon(Icons.Default.Search, null)

@Composable
fun PlusDot(onClick: (() -> Unit)? = null) = Box(
    Modifier.size(40.dp).clip(CircleShape).background(C.Sky).let { if (onClick != null) it.clickable(onClick = onClick) else it },
    Alignment.Center
) { Icon(Icons.Default.Add, "Agregar", tint = Color.White) }

// ───────────── Cabecera cian redondeada ─────────────
@Composable
fun Cabecera(titulo: String, nav: NavController, vm: AppViewModel, conAtras: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(horizontal = 24.dp, vertical = 36.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (conAtras) BackBtn { nav.popBackStack() } else Spacer(Modifier.size(52.dp))
        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        BellBtn(vm.sinLeer) { nav.navigate("notificaciones") }
    }
}

/** ‹ Agosto de 2026 › — cambia de mes y recarga los datos desde MySQL */
@Composable
fun SelectorMes(vm: AppViewModel, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text("‹", fontSize = 34.sp, modifier = Modifier.clip(CircleShape).clickable { vm.cambiarMes(-1) }.padding(horizontal = 18.dp))
        Text(vm.mes.largo(), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.widthIn(min = 170.dp), textAlign = TextAlign.Center)
        Text("›", fontSize = 34.sp, modifier = Modifier.clip(CircleShape).clickable { vm.cambiarMes(1) }.padding(horizontal = 18.dp))
    }
}

// ───────────── Campos de texto (crema, redondeados) ─────────────
@Composable
fun Campo(
    valor: String, onChange: (String) -> Unit, etiqueta: String, modifier: Modifier = Modifier,
    teclado: KeyboardOptions = KeyboardOptions.Default, transformacion: VisualTransformation = VisualTransformation.None,
    error: Boolean = false, soloLectura: Boolean = false, singleLine: Boolean = true, trailing: @Composable (() -> Unit)? = null
) = OutlinedTextField(
    value = valor, onValueChange = onChange, label = { Text(etiqueta) }, singleLine = singleLine,
    keyboardOptions = teclado, visualTransformation = transformacion, isError = error, readOnly = soloLectura,
    trailingIcon = trailing, shape = RoundedCornerShape(20.dp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = C.Cream, unfocusedContainerColor = C.Cream, errorContainerColor = C.Cream,
        focusedBorderColor = C.Teal, unfocusedBorderColor = Color.Transparent, errorBorderColor = C.Pink,
        focusedLabelColor = C.Teal
    ),
    modifier = modifier.fillMaxWidth()
)

@Composable
fun Aviso(texto: String, ok: Boolean = false) =
    Text(texto, color = if (ok) C.Teal else C.Pink, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())

// ───────────── Tarjeta de movimiento (como en los mockups) ─────────────
@Composable
fun MovCard(m: Mov, modifier: Modifier = Modifier, mostrarTipo: Boolean = false, onClick: () -> Unit = {}) {
    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(if (m.entrada) C.LightGreen else C.Red)
            .clickable(onClick = onClick).padding(14.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color.White), Alignment.Center) {
            if (m.imagen != null) {
                AsyncImage(model = m.imagen, contentDescription = m.nombre, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(m.emoji, fontSize = 52.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(m.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (mostrarTipo) "• " + (if (m.entrada) "ENTRADA" else "GASTO") else m.subtitulo,
            fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(m.monto.cop(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            PlusDot()
        }
    }
}

// ───────────── Dona de gastos / saldo ─────────────
/** Un arco por gasto + el saldo restante (naranja), igual que en los mockups. Sin datos → anillo gris. */
@Composable
fun Donut(gastos: List<Pair<Long, Color>>, saldo: Long, modifier: Modifier = Modifier, grosor: Float = 130f) {
    val saldoPositivo = saldo.coerceAtLeast(0)
    val total = gastos.sumOf { it.first } + saldoPositivo
    Canvas(modifier) {
        val inset = grosor / 2
        val tl = Offset(inset, inset)
        val sz = Size(size.width - grosor, size.height - grosor)
        if (total <= 0L) {
            drawArc(Color(0xFFE3EAEC), 0f, 360f, false, tl, sz, style = Stroke(grosor))
            return@Canvas
        }
        var start = -90f
        (gastos + (saldoPositivo to C.Orange)).forEach { (v, col) ->
            val sweep = 360f * v / total
            if (sweep > 0f) drawArc(col, start, sweep, false, tl, sz, style = Stroke(grosor))
            start += sweep
        }
    }
}

val GastoColors = listOf(C.Indigo, C.Pink, C.Teal, C.Sky, C.Green)

// ───────────── Barra inferior (menú con un solo dedo) ─────────────
@Composable
fun BarraInferior(nav: NavController, ruta: String?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.Cyan)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround
    ) {
        ItemBarra("🏠", "Inicio", ruta == "home") { ir(nav, "home") }
        ItemBarra("📊", "Gráfico", ruta == "grafico") { ir(nav, "grafico") }
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(C.Sky).clickable { nav.navigate("movimiento") },
            Alignment.Center
        ) { Text("+", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
        ItemBarra("📄", "Extracto", ruta == "extracto") { ir(nav, "extracto") }
        ItemBarra("👤", "Perfil", ruta == "perfil") { ir(nav, "perfil") }
    }
}

private fun ir(nav: NavController, destino: String) {
    nav.navigate(destino) { popUpTo("home"); launchSingleTop = true }
}

@Composable
private fun ItemBarra(emoji: String, texto: String, activo: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(if (activo) Color.White.copy(alpha = .6f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(texto, fontSize = 11.sp, fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal)
    }
}
