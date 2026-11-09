package com.platapami.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ───────────── 3. LISTA DE MOVIMIENTOS ─────────────
@Composable
fun MovimientosScreen(nav: NavController, vm: AppViewModel, filtroInicial: String) {
    var filtro by remember { mutableStateOf(filtroInicial) }
    var query by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf<Mov?>(null) }
    val lista = vm.movs.filter {
        (filtro == "todos" || (filtro == "entradas") == it.entrada) &&
            (query.isBlank() || it.nombre.contains(query, true) || it.detalle.contains(query, true) || Categorias.nombre(it.categoria).contains(query, true))
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
            BellBtn(vm.sinLeer) { nav.navigate("notificaciones") }
        }
        Cargando(vm)
        SelectorMes(vm)
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Chip("Entradas", C.LightGreen, filtro == "entradas") { filtro = "entradas" }
            Chip("Salidas", C.Red, filtro == "salidas") { filtro = "salidas" }
            Chip("Todos", C.Sky, filtro == "todos", Color.White) { filtro = "todos" }
        }
        if (lista.isEmpty() && !vm.cargando) {
            Text(
                if (vm.movs.isEmpty()) "No hay movimientos en ${vm.mes.largo()}.\nToca + para agregar uno." else "Nada coincide con tu búsqueda.",
                Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = C.Grey
            )
        }
        LazyVerticalGrid(
            GridCells.Fixed(2), modifier = Modifier.weight(1f), contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)
        ) { items(lista, key = { it.id }) { m -> MovCard(m) { detalle = m } } }
    }
    detalle?.let { MovDetalleDialog(it, nav, vm) { detalle = null } }
}

@Composable
private fun Chip(t: String, c: Color, sel: Boolean, tc: Color = Color.Black, onClick: () -> Unit) =
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(c)
            .border(if (sel) BorderStroke(2.dp, Color.Black) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp)
    ) { Text(t, color = tc, fontSize = 18.sp, fontWeight = FontWeight.Light) }

// ───────────── Detalle (ver / editar / eliminar) ─────────────
@Composable
fun MovDetalleDialog(m: Mov, nav: NavController, vm: AppViewModel, onClose: () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var borrando by remember { mutableStateOf(false) }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { if (!borrando) confirmar = false },
            title = { Text("¿Eliminar movimiento?", fontWeight = FontWeight.Bold) },
            text = { Text("“${m.nombre}” por ${m.monto.cop()} se borrará de tu registro y de tu presupuesto.") },
            confirmButton = {
                TextButton(enabled = !borrando, onClick = {
                    borrando = true
                    vm.eliminarMov(m.id) { err -> borrando = false; if (err == null) onClose() else { error = err; confirmar = false } }
                }) { Text("Eliminar", color = C.Pink, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(enabled = !borrando, onClick = { confirmar = false }) { Text("Cancelar") } },
            containerColor = Color.White
        )
        return
    }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Color.White,
        title = { Text(m.nombre, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(if (m.entrada) C.LightGreen else C.Red),
                    Alignment.Center
                ) {
                    if (m.imagen != null) AsyncImage(m.imagen, m.nombre, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text(m.emoji, fontSize = 64.sp)
                }
                Text(m.monto.cop(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = if (m.entrada) C.Green else C.Pink)
                Text(if (m.entrada) "Entrada" else "Gasto", fontSize = 14.sp)
                Text("Categoría: ${Categorias.nombre(m.categoria)}", fontSize = 14.sp)
                if (m.detalle.isNotBlank()) Text("Detalle: ${m.detalle}", fontSize = 14.sp)
                Text("Fecha: ${m.fecha.fechaCorta()}", fontSize = 14.sp)
                error?.let { Aviso(it) }
            }
        },
        confirmButton = { TextButton(onClick = { onClose(); nav.navigate("movimiento/${m.id}") }) { Text("Editar", color = C.Teal, fontWeight = FontWeight.Bold) } },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmar = true }) { Text("Eliminar", color = C.Pink) }
                TextButton(onClick = onClose) { Text("Cerrar") }
            }
        }
    )
}

// ───────────── 4. NUEVO / EDITAR MOVIMIENTO ─────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientoFormScreen(nav: NavController, vm: AppViewModel, editId: Long?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val editando = remember(editId) { editId?.let { id -> vm.movs.firstOrNull { it.id == id } } }

    var entrada by rememberSaveable { mutableStateOf(editando?.entrada ?: false) }
    var nombre by rememberSaveable { mutableStateOf(editando?.nombre ?: "") }
    var detalle by rememberSaveable { mutableStateOf(editando?.detalle ?: "") }
    var categoria by rememberSaveable { mutableStateOf(editando?.categoria ?: "otros") }
    var montoTxt by rememberSaveable { mutableStateOf(editando?.monto?.toString() ?: "") }
    var fecha by rememberSaveable { mutableStateOf(editando?.fecha ?: LocalDate.now().toString()) }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }
    var quitarImagen by remember { mutableStateOf(false) }
    var camaraUri by remember { mutableStateOf<Uri?>(null) }
    var verPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var avisoMonto by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { imagenUri = uri; quitarImagen = false }
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) { imagenUri = camaraUri; quitarImagen = false }
    }

    val monto = montoTxt.toLongOrNull() ?: 0L
    val colorTipo = if (entrada) C.Green else C.Pink
    val imagenActual: Any? = imagenUri ?: if (!quitarImagen) editando?.imagen else null

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().clip(HeaderShape).background(C.Cyan).padding(horizontal = 24.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackBtn { nav.popBackStack() }
            Spacer(Modifier.width(16.dp))
            Text(if (editando != null) "EDITAR REGISTRO" else "NUEVO REGISTRO", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TipoBtn("Entrada", C.Green, entrada, Modifier.weight(1f)) { entrada = true }
                TipoBtn("Gasto", C.Pink, !entrada, Modifier.weight(1f)) { entrada = false }
            }
            Campo(nombre, { nombre = it.take(80) }, "Nombre (ej. Café, Salario)")
            Campo(detalle, { detalle = it.take(120) }, "Detalle (ej. Juan Valdez)")

            Text("Categoría", fontSize = 13.sp, color = C.Grey)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categorias.todas.forEach { (clave, nom, emo) ->
                    Text(
                        "$emo $nom", fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (categoria == clave) C.Cyan else C.Cream)
                            .border(if (categoria == clave) 2.dp else 0.dp, if (categoria == clave) C.Teal else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { categoria = clave }.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Campo(
                montoTxt, { montoTxt = it.filter(Char::isDigit).trimStart('0').take(13) }, "Valor (COP)",
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number), error = avisoMonto && monto <= 0
            )
            if (monto > 0) Text("= ${monto.cop()}", color = colorTipo, fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Cream)
                    .clickable { verPicker = true }.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Fecha", fontSize = 12.sp, color = C.Grey)
                    Text(fecha.fechaCorta(), fontSize = 16.sp)
                }
                Text("📅", fontSize = 22.sp)
            }

            Text("Comprobante (opcional)", fontSize = 13.sp, color = C.Grey)
            if (imagenActual != null) {
                AsyncImage(
                    imagenActual, "Comprobante", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(20.dp))
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("📷 Cámara", C.Teal, Modifier.weight(1f)) {
                    val f = File(File(ctx.cacheDir, "comprobantes").apply { mkdirs() }, "cam_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                    camaraUri = uri
                    camara.launch(uri)
                }
                Btn("🖼️ Galería", C.Indigo, Modifier.weight(1f)) {
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            if (imagenActual != null) {
                Text("Quitar imagen", color = C.Pink, fontSize = 14.sp, modifier = Modifier.clickable { imagenUri = null; quitarImagen = true }.padding(4.dp))
            }

            error?.let { Aviso(it) }
            Btn(if (editando != null) "Guardar cambios" else "Guardar", colorTipo, Modifier.fillMaxWidth(), loading = guardando) {
                error = null
                when {
                    nombre.isBlank() -> error = "Escribe un nombre para el movimiento"
                    monto <= 0 -> { avisoMonto = true }
                    else -> {
                        guardando = true
                        scope.launch {
                            val b64 = imagenUri?.let { withContext(Dispatchers.IO) { comprimirImagen(ctx, it) } }
                            if (imagenUri != null && b64 == null) {
                                guardando = false; error = "No se pudo leer la imagen. Elige otra."
                                return@launch
                            }
                            vm.guardarMov(
                                editando?.id, entrada, nombre, detalle, categoria, monto, LocalDate.parse(fecha),
                                b64, quitarImagen && imagenUri == null
                            ) { err -> guardando = false; if (err == null) nav.popBackStack() else error = err }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    // Aviso antes de guardar si falta el valor (ficha técnica: "evitar datos incompletos")
    if (avisoMonto && monto <= 0) {
        AlertDialog(
            onDismissRequest = { avisoMonto = false },
            containerColor = Color.White,
            title = { Text("Falta el valor", fontWeight = FontWeight.Bold) },
            text = { Text("Olvidaste poner el precio de este ${if (entrada) "ingreso" else "gasto"}. Escríbelo para guardar el registro completo.") },
            confirmButton = { TextButton(onClick = { avisoMonto = false }) { Text("Entendido", color = C.Teal, fontWeight = FontWeight.Bold) } }
        )
    }

    if (verPicker) {
        val st = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { verPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    verPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { verPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = st) }
    }
}

@Composable
private fun TipoBtn(t: String, c: Color, sel: Boolean, modifier: Modifier, onClick: () -> Unit) =
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (sel) c else C.Cream)
            .border(if (sel) 0.dp else 2.dp, c, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        Alignment.Center
    ) { Text(t, fontSize = 18.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Light, color = if (sel) Color.White else c) }

/** Reduce la foto (máx. ~1000 px, JPEG 80 %), corrige la rotación y la devuelve en Base64 para subirla a la API. */
fun comprimirImagen(ctx: Context, uri: Uri, maxLado: Int = 1000): String? {
    try {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxLado * 2 || bounds.outHeight / sample > maxLado * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val orient = cr.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val grados = when (orient) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val escala = minOf(1f, maxLado.toFloat() / maxOf(bmp.width, bmp.height))
        if (grados != 0f || escala < 1f) {
            val m = Matrix().apply { postRotate(grados); postScale(escala, escala) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        return null
    }
}
