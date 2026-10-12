package br.com.dashcamadas

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Eventos locais: registros de teste são identificados como SIMULAÇÃO. */
class EventStore(context: Context) {
 private val prefs = context.getSharedPreferences("eventos",Context.MODE_PRIVATE)
 fun all(): JSONArray = try { JSONArray(prefs.getString("items","[]")) } catch (_:Exception) { JSONArray() }
 fun add(type:String, details:String, source:String = "MANUAL / TESTE", confirmed:Boolean = false) = synchronized(lock) {
  val events = all(); events.put(JSONObject().apply {
   put("timestamp",System.currentTimeMillis());put("tipo",type)
   put("detalhes",details);put("origem",source);put("confirmado",confirmed)
  })
  while(events.length()>250) events.remove(0)
  prefs.edit().putString("items",events.toString()).apply()
 }
 fun lines(): List<String> { val a=all(); val df=SimpleDateFormat("dd/MM HH:mm:ss",Locale("pt","BR"));
  return (a.length()-1 downTo 0).map { val o=a.getJSONObject(it); "${df.format(Date(o.getLong("timestamp")))} • ${o.optString("tipo")}\n${o.optString("detalhes")} • ${o.optString("origem")}" }
 }
 fun clear()=synchronized(lock){prefs.edit().remove("items").apply()}
 companion object {private val lock=Any()}
}
