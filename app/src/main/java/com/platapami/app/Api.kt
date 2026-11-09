package com.platapami.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error "con mensaje listo para mostrar al usuario". */
class ApiException(message: String, val status: Int = 0, val campo: String? = null) : Exception(message)

/** Token de sesión y datos básicos del usuario, guardados en el almacenamiento privado de la app. */
object Session {
    private var prefs: SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs == null) prefs = ctx.applicationContext.getSharedPreferences("platapami_session", Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs?.getString("token", null)
        set(v) { prefs?.edit()?.apply { if (v == null) remove("token") else putString("token", v) }?.apply() }

    fun guardarUsuario(json: JSONObject) { prefs?.edit()?.putString("usuario", json.toString())?.apply() }

    fun usuario(): Usuario? {
        if (token == null) return null
        val s = prefs?.getString("usuario", null) ?: return null
        return try { JSONObject(s).toUsuario() } catch (e: Exception) { null }
    }

    /** Copia local del último mes descargado: sin internet se sigue viendo (solo lectura). */
    fun guardarMes(ym: String, json: String) { prefs?.edit()?.putString("mes_$ym", json)?.apply() }
    fun leerMes(ym: String): String? = prefs?.getString("mes_$ym", null)

    fun limpiar() { prefs?.edit()?.clear()?.apply() }
}

/** Cliente de la API PHP (JSON sobre HTTP/HTTPS). */
object Api {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)     // el envío del extracto (PDF + SMTP) puede tardar
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun request(
        method: String,
        route: String,
        query: Map<String, String> = emptyMap(),
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val url = (BuildConfig.API_BASE + "index.php").toHttpUrl().newBuilder().apply {
            addQueryParameter("r", route)
            query.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()

        val payload = (body ?: JSONObject()).toString().toRequestBody(JSON)
        val req = Request.Builder().url(url).header("Accept", "application/json").apply {
            Session.token?.let { header("Authorization", "Bearer $it") }
            when (method) {
                "GET" -> get()
                "POST" -> post(payload)
                "PUT" -> put(payload)
                "DELETE" -> delete()
                else -> throw IllegalArgumentException(method)
            }
        }.build()

        try {
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = try { JSONObject(text) } catch (e: Exception) {
                    throw ApiException("El servidor respondió algo inesperado (${resp.code}). Revisa la configuración del backend.", resp.code)
                }
                if (!json.optBoolean("ok", false)) {
                    throw ApiException(
                        json.optString("error", "Ocurrió un error. Intenta de nuevo."),
                        resp.code,
                        json.optString("campo").ifBlank { null }
                    )
                }
                json
            }
        } catch (e: IOException) {
            throw ApiException("No hay conexión con el servidor. Revisa tu internet e intenta de nuevo.")
        }
    }
}
