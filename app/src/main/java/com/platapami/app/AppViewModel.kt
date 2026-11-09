package com.platapami.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

/**
 * Estado de toda la app. Los datos viven en MySQL (vía API PHP); aquí solo está lo que se muestra.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    init { Session.init(app) }

    // ───────────── Sesión ─────────────
    /** null = no hay sesión iniciada */
    var usuario by mutableStateOf(Session.usuario())
        private set

    /** Decisión del usuario sobre Términos y Habeas Data: null = aún no decide */
    var terminos by mutableStateOf<Boolean?>(null)

    // ───────────── Mes en pantalla ─────────────
    var mes by mutableStateOf(YearMonth.now())
        private set
    private var siguiendoMesActual = true

    var movs by mutableStateOf<List<Mov>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var errorCarga by mutableStateOf<String?>(null)
        private set
    /** true = no hay internet y se muestran los datos guardados en el celular (solo lectura) */
    var sinConexion by mutableStateOf(false)
        private set

    var notifs by mutableStateOf<List<Notif>>(emptyList())
        private set
    var sinLeer by mutableStateOf(0)
        private set

    val entradas: Long get() = movs.filter { it.entrada }.sumOf { it.monto }
    val gastos: Long get() = movs.filter { !it.entrada }.sumOf { it.monto }
    val saldo: Long get() = entradas - gastos

    init { if (usuario != null) { cargarMes(); cargarNotifs(); refrescarUsuario() } }

    // ───────────── Datos del mes ─────────────
    fun cargarMes() {
        if (usuario == null) return
        viewModelScope.launch {
            cargando = true
            errorCarga = null
            try {
                val j = Api.request("GET", "movements", mapOf("month" to mes.toString()))
                val arr = j.getJSONArray("movimientos")
                movs = arr.toMovs()
                sinConexion = false
                Session.guardarMes(mes.toString(), arr.toString())
            } catch (e: ApiException) {
                val copia = if (e.status == 0) Session.leerMes(mes.toString()) else null
                if (copia != null) {
                    // Sin internet: se muestra la última copia guardada de ese mes
                    movs = JSONArray(copia).toMovs()
                    sinConexion = true
                } else {
                    sinConexion = false
                    manejar(e) { errorCarga = it }
                }
            } finally {
                cargando = false
            }
        }
    }

    fun cambiarMes(delta: Long) {
        mes = mes.plusMonths(delta)
        siguiendoMesActual = mes == YearMonth.now()
        cargarMes()
    }

    /** Se llama cada vez que la app vuelve al frente: si cambió el mes, el Home pasa solo al mes nuevo. */
    fun onResume() {
        if (usuario == null) return
        if (siguiendoMesActual && mes != YearMonth.now()) mes = YearMonth.now()
        cargarMes()
        cargarNotifs()
    }

    private fun refrescarUsuario() {
        viewModelScope.launch {
            try {
                val j = Api.request("GET", "me")
                val u = j.getJSONObject("usuario")
                Session.guardarUsuario(u)
                usuario = u.toUsuario()
            } catch (e: ApiException) {
                manejar(e) { }
            }
        }
    }

    // ───────────── Autenticación ─────────────
    fun login(correo: String, clave: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        try {
            val j = Api.request("POST", "auth/login", body = JSONObject().put("correo", correo.trim()).put("password", clave))
            abrirSesion(j)
            onDone(null)
        } catch (e: ApiException) { onDone(e.message) }
    }

    fun registrar(
        nombre: String, tipoDoc: String, documento: String, correo: String, clave: String,
        onDone: (error: String?, campo: String?) -> Unit
    ) = viewModelScope.launch {
        try {
            val body = JSONObject()
                .put("nombre", nombre.trim()).put("tipo_documento", tipoDoc).put("documento", documento.trim())
                .put("correo", correo.trim()).put("password", clave)
                .put("acepta_terminos", terminos == true)
            val j = Api.request("POST", "auth/register", body = body)
            abrirSesion(j)
            terminos = null
            onDone(null, null)
        } catch (e: ApiException) { onDone(e.message, e.campo) }
    }

    fun olvideContrasena(correo: String, onDone: (mensaje: String?, error: String?) -> Unit) = viewModelScope.launch {
        try {
            val j = Api.request("POST", "auth/forgot", body = JSONObject().put("correo", correo.trim()))
            onDone(j.optString("mensaje"), null)
        } catch (e: ApiException) { onDone(null, e.message) }
    }

    fun cerrarSesion() {
        viewModelScope.launch { try { Api.request("POST", "auth/logout") } catch (e: ApiException) { } }
        limpiarLocal()
    }

    private fun abrirSesion(j: JSONObject) {
        Session.token = j.getString("token")
        val u = j.getJSONObject("usuario")
        Session.guardarUsuario(u)
        usuario = u.toUsuario()
        mes = YearMonth.now()
        siguiendoMesActual = true
        movs = emptyList()
        cargarMes()
        cargarNotifs()
    }

    private fun limpiarLocal() {
        Session.limpiar()
        usuario = null
        movs = emptyList()
        notifs = emptyList()
        sinLeer = 0
        errorCarga = null
        sinConexion = false
    }

    /** Derecho de supresión (Habeas Data): borra la cuenta y todos sus datos en el servidor. */
    fun eliminarCuenta(clave: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        try {
            Api.request("POST", "me/delete", body = JSONObject().put("password", clave))
            limpiarLocal()
            onDone(null)
        } catch (e: ApiException) { onDone(e.message) }
    }

    /** Si el servidor dice 401 (sesión vencida) se cierra la sesión; si no, se muestra el mensaje. */
    private fun manejar(e: ApiException, mostrar: (String?) -> Unit) {
        if (e.status == 401) limpiarLocal() else mostrar(e.message)
    }

    // ───────────── Movimientos ─────────────
    fun guardarMov(
        id: Long?, entrada: Boolean, nombre: String, detalle: String, categoria: String, monto: Long,
        fecha: LocalDate, imagenBase64: String?, quitarImagen: Boolean,
        onDone: (String?) -> Unit
    ) = viewModelScope.launch {
        try {
            val body = JSONObject()
                .put("tipo", if (entrada) "entrada" else "gasto").put("nombre", nombre.trim()).put("detalle", detalle.trim())
                .put("categoria", categoria).put("monto", monto).put("fecha", fecha.toString())
            if (imagenBase64 != null) body.put("imagen_base64", imagenBase64)
            if (quitarImagen) body.put("quitar_imagen", true)
            if (id == null) Api.request("POST", "movements", body = body) else Api.request("PUT", "movements/$id", body = body)
            // Se muestra el mes del movimiento guardado, para que el usuario lo vea de inmediato.
            mes = YearMonth.from(fecha)
            siguiendoMesActual = mes == YearMonth.now()
            cargarMes()
            cargarNotifs()
            onDone(null)
        } catch (e: ApiException) {
            if (e.status == 401) limpiarLocal()
            onDone(e.message)
        }
    }

    fun eliminarMov(id: Long, onDone: (String?) -> Unit) = viewModelScope.launch {
        try {
            Api.request("DELETE", "movements/$id")
            cargarMes()
            onDone(null)
        } catch (e: ApiException) {
            if (e.status == 401) limpiarLocal()
            onDone(e.message)
        }
    }

    // ───────────── Notificaciones ─────────────
    fun cargarNotifs() {
        if (usuario == null) return
        viewModelScope.launch {
            try {
                val j = Api.request("GET", "notifications")
                notifs = j.getJSONArray("notificaciones").toNotifs()
                sinLeer = j.optInt("sin_leer", 0)
            } catch (e: ApiException) { manejar(e) { } }
        }
    }

    fun marcarLeidas() {
        if (sinLeer == 0) return
        viewModelScope.launch {
            try {
                Api.request("POST", "notifications/read")
                sinLeer = 0
                notifs = notifs.map { it.copy(leida = true) }
            } catch (e: ApiException) { manejar(e) { } }
        }
    }

    // ───────────── Extracto ─────────────
    fun enviarExtracto(ym: YearMonth, onDone: (ok: Boolean, mensaje: String) -> Unit) = viewModelScope.launch {
        try {
            val j = Api.request("POST", "statements/send", body = JSONObject().put("month", ym.toString()))
            cargarNotifs()
            onDone(true, j.optString("mensaje", "Extracto enviado a tu correo."))
        } catch (e: ApiException) {
            if (e.status == 401) limpiarLocal()
            onDone(false, e.message ?: "No se pudo enviar el extracto.")
        }
    }
}
