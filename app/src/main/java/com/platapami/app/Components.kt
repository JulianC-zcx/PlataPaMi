package com.platapami.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val HeaderShape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp)

@Composable
fun Btn(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(color).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light) }
}

@Composable
fun CircleBtn(modifier: Modifier = Modifier, onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box(modifier.size(52.dp).clip(CircleShape).background(Color.White).clickable(onClick = onClick), Alignment.Center) { content() }
}

@Composable
fun BackBtn(onClick: () -> Unit) = CircleBtn(onClick = onClick) { Icon(Icons.Default.ArrowBack, "Atrás") }

@Composable
fun BellBtn() = CircleBtn {
    Box {
        Icon(Icons.Default.Notifications, "Notificaciones")
        Box(Modifier.size(10.dp).clip(CircleShape).background(C.Pink).align(Alignment.TopEnd))
    }
}

@Composable
fun SearchIcon() = Icon(Icons.Default.Search, null)

@Composable
fun PlusDot() = Box(Modifier.size(40.dp).clip(CircleShape).background(C.Sky), Alignment.Center) {
    Icon(Icons.Default.Add, "Agregar", tint = Color.White)
}

@Composable
fun MovCard(m: Mov, modifier: Modifier = Modifier, mostrarTipo: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(if (m.entrada) C.LightGreen else C.Red).padding(14.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color.White), Alignment.Center) {
            Text(m.emoji, fontSize = 52.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(m.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
        Text(if (mostrarTipo) "• " + (if (m.entrada) "ENTRADA" else "GASTO") else m.detalle, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(m.monto.cop(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            PlusDot()
        }
    }
}

/** Dona: cada gasto + saldo restante (como en los mockups). */
@Composable
fun Donut(gastos: List<Pair<Long, Color>>, saldo: Long, modifier: Modifier = Modifier, grosor: Float = 130f) {
    val total = (gastos.sumOf { it.first } + saldo).coerceAtLeast(1)
    Canvas(modifier) {
        var start = -90f
        val inset = grosor / 2
        val tl = Offset(inset, inset)
        val sz = Size(size.width - grosor, size.height - grosor)
        (gastos + (saldo to C.Orange)).forEach { (v, col) ->
            val sweep = 360f * v / total
            drawArc(col, start, sweep, false, tl, sz, style = Stroke(grosor))
            start += sweep
        }
    }
}

val GastoColors = listOf(C.Indigo, C.Pink, C.Teal, C.Sky, C.Green)
