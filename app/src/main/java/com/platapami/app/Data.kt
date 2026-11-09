package com.platapami.app

import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

// ───────────── Modelos ─────────────
data class Mov(
    val id: Long,
    val nombre: String,
    val detalle: String,
    val monto: Long,
    val entrada: Boolean,
    val categoria: String,
    val fecha: String,      // AAAA-MM-DD
    val imagen: String?     // URL del comprobante (o null)
) {
    val emoji: String get() = Categorias.emoji(categoria)
    /** Texto bajo el nombre en las tarjetas (como en los mockups: "Crepes", "JC Industries"…) */
    val subtitulo: String get() = detalle.ifBlank { Categorias.nombre(categoria) }
}

data class Usuario(val nombre: String, val correo: String, val tipoDocumento: String, val documentoMask: String) {
    val primerNombre: String get() = nombre.trim().split(" ").firstOrNull().orEmpty()
}

data class Notif(val id: Long, val tipo: String, val titulo: String, val mensaje: String, val leida: Boolean, val creado: String)

object Categorias {
    /** clave del servidor → (nombre visible, emoji) */
    val todas: List<Triple<String, String, String>> = listOf(
        Triple("comida", "Comida", "🍽️"),
        Triple("transporte", "Transporte", "🚌"),
        Triple("hogar", "Hogar", "🏠"),
        Triple("ocio", "Ocio", "🎬"),
        Triple("salud", "Salud", "💊"),
        Triple("educacion", "Educación", "📚"),
        Triple("compras", "Compras", "🛍️"),
        Triple("servicios", "Servicios", "💡"),
        Triple("salario", "Salario", "💵"),
        Triple("otros", "Otros", "🪙")
    )
    fun emoji(clave: String) = todas.firstOrNull { it.first == clave }?.third ?: "🪙"
    fun nombre(clave: String) = todas.firstOrNull { it.first == clave }?.second ?: "Otros"
}

// ───────────── JSON → modelos ─────────────
fun JSONObject.toMov() = Mov(
    id = getLong("id"),
    nombre = getString("nombre"),
    detalle = optString("detalle", ""),
    monto = getLong("monto"),
    entrada = getString("tipo") == "entrada",
    categoria = optString("categoria", "otros"),
    fecha = getString("fecha"),
    imagen = if (isNull("imagen")) null else optString("imagen").ifBlank { null }?.let { urlComprobante(it) }
)

/** El servidor manda "uploads/abc.jpg"; se une a la URL base de la API (…/public/api/ → …/public/). */
fun urlComprobante(ruta: String): String =
    if (ruta.startsWith("http")) ruta else BuildConfig.API_BASE.removeSuffix("api/") + ruta

fun JSONArray.toMovs(): List<Mov> = List(length()) { getJSONObject(it).toMov() }

fun JSONObject.toUsuario() = Usuario(
    nombre = optString("nombre"), correo = optString("correo"),
    tipoDocumento = optString("tipo_documento", "CC"), documentoMask = optString("documento_mask")
)

fun JSONArray.toNotifs(): List<Notif> = List(length()) {
    val o = getJSONObject(it)
    Notif(o.getLong("id"), o.optString("tipo"), o.optString("titulo"), o.optString("mensaje"), o.optBoolean("leida"), o.optString("creado_at"))
}

// ───────────── Formatos ─────────────
private val LOCALE_CO = Locale("es", "CO")
private val MES3 = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
private val MESES = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

fun Long.cop(): String = (if (this < 0) "-" else "") + "$ " + NumberFormat.getInstance(LOCALE_CO).format(kotlin.math.abs(this))

fun pct(parte: Long, total: Long): String =
    if (total <= 0) "0%" else String.format(LOCALE_CO, "%.1f%%", parte * 100.0 / total)

/** "AGO 01 - AGO 31 / 2026" (cabecera del Home; cambia sola cada mes) */
fun YearMonth.rango(): String {
    val m = MES3[monthValue - 1]
    return "$m 01 - $m ${lengthOfMonth().toString().padStart(2, '0')} / $year"
}

/** "Agosto de 2026" */
fun YearMonth.largo(): String = "${MESES[monthValue - 1]} de $year"

/** "2026-08-05" → "05 ago 2026" */
fun String.fechaCorta(): String {
    val p = split("-")
    if (p.size != 3) return this
    return "${p[2]} ${MES3[(p[1].toIntOrNull() ?: 1) - 1].lowercase()} ${p[0]}"
}
