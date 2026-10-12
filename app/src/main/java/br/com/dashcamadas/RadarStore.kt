package br.com.dashcamadas

import android.content.Context
import android.location.Location
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Pontos de fiscalização informados manualmente; não detecta radiofrequência. */
data class RadarPoint(val id: String, val lat: Double, val lon: Double, val limit: Int,
                      val type: String, val direction: Float?, val created: Long)

class RadarStore(context: Context) {
    private val prefs = context.getSharedPreferences("radar_points", Context.MODE_PRIVATE)
    fun all(): List<RadarPoint> = try {
        val items = JSONArray(prefs.getString("points", "[]"))
        (0 until items.length()).map { i ->
            val p = items.getJSONObject(i)
            RadarPoint(p.getString("id"),p.getDouble("lat"),p.getDouble("lon"),p.getInt("limit"),
                p.getString("type"),if(p.isNull("direction")) null else p.getDouble("direction").toFloat(),p.getLong("created"))
        }
    } catch (_: Exception) { emptyList() }
    fun add(location: Location, limit: Int, type: String) {
        require(limit in 10..140)
        val direction = if (location.hasBearing() && location.speed > 2f) location.bearing else null
        val item = RadarPoint(UUID.randomUUID().toString(),location.latitude,location.longitude,limit,type,direction,System.currentTimeMillis())
        save(all() + item)
    }
    fun remove(id: String) = save(all().filterNot { it.id == id })
    private fun save(points: List<RadarPoint>) {
        val array = JSONArray()
        points.forEach { p -> array.put(JSONObject().apply {
            put("id",p.id);put("lat",p.lat);put("lon",p.lon);put("limit",p.limit)
            put("type",p.type);put("direction",p.direction ?: JSONObject.NULL);put("created",p.created)
        }) }
        prefs.edit().putString("points",array.toString()).apply()
    }
}

data class RadarAlert(val radar: RadarPoint, val distanceMeters: Int) {
    val spokenMessage get() = "Radar cadastrado de ${radar.limit} quilômetros por hora a $distanceMeters metros"
}

class RadarMonitor(private val store: RadarStore, private val alert: (RadarAlert) -> Unit) {
    private val lastAlert = mutableMapOf<String,Long>()
    fun onLocation(location: Location) {
        if (location.accuracy > 80f || !location.hasSpeed()) return
        val now = System.currentTimeMillis()
        store.all().forEach { radar ->
            val result = FloatArray(2)
            Location.distanceBetween(location.latitude,location.longitude,radar.lat,radar.lon,result)
            val distance = result[0]
            if (distance > 500f || distance < 20f) return@forEach
            if (location.speed >= 2f && location.hasBearing()) {
                val diff = kotlin.math.abs((result[1] - location.bearing + 540f) % 360f - 180f)
                if (diff > 55f) return@forEach // Radar à frente, não atrás ou em rua lateral
            }
            if (radar.direction != null && location.hasBearing()) {
                val diff = kotlin.math.abs((radar.direction - location.bearing + 540f) % 360f - 180f)
                if (diff > 70f) return@forEach
            }
            if (now - (lastAlert[radar.id] ?: 0L) > 120_000L) {
                lastAlert[radar.id] = now
                alert(RadarAlert(radar, distance.toInt()))
            }
        }
    }
}
