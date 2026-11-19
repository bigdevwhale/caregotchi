package app.caregotchi.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.ZoneId
import java.util.Locale

/** What the round window shows. */
enum class Weather { CLEAR, CLOUDY, OVERCAST, RAIN, SNOW, FOG, STORM }

/**
 * The weather outside right now: the sky, the temperature and the air pressure (hPa, at sea level),
 * with the pressure for the next hours ([ahead], one an hour) to warn of a fall.
 */
data class Outside(val weather: Weather, val temp: Double, val pressure: Double, val ahead: List<Double>, val at: Long) {
    /** The biggest fall in the next 18 hours, hPa (negative), or 0. */
    val fall: Double get() = if (pressure.isNaN() || ahead.isEmpty()) 0.0 else (ahead.take(18).minOrNull()!! - pressure).coerceAtMost(0.0)

    fun json(): String = JSONObject().put("w", weather.name).put("t", temp).put("p", pressure).put("a", JSONArray(ahead)).put("at", at).toString()

    companion object {
        fun of(s: String?): Outside? = runCatching {
            val o = JSONObject(s ?: return null)
            val a = o.optJSONArray("a")
            Outside(Weather.valueOf(o.getString("w")), o.optDouble("t"), o.optDouble("p"), a?.let { (0 until it.length()).map { i -> it.getDouble(i) } } ?: emptyList(), o.getLong("at"))
        }.getOrNull()
    }
}

data class Town(val name: String, val region: String, val lat: Double, val lon: Double)

/** Open-Meteo, or MET Norway when Open-Meteo can't be reached. Nothing but the town's coordinates is sent. */
object OutsideSource {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 7000; c.readTimeout = 9000
        c.setRequestProperty("User-Agent", "Caregotchi/1.0 (https://github.com/bigdevwhale/caregotchi)")
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    fun fetch(lat: Double, lon: Double): Outside = runCatching { openMeteo(lat, lon) }.getOrElse { metNo(lat, lon) }

    private fun ofCode(code: Int, cloud: Int): Weather = when (code) {
        0, 1 -> if (cloud > 60) Weather.CLOUDY else Weather.CLEAR
        2 -> Weather.CLOUDY
        3 -> Weather.OVERCAST
        45, 48 -> Weather.FOG
        in 51..67, in 80..82 -> Weather.RAIN
        in 71..77, 85, 86 -> Weather.SNOW
        in 95..99 -> Weather.STORM
        else -> if (cloud > 70) Weather.OVERCAST else Weather.CLOUDY
    }

    private fun openMeteo(lat: Double, lon: Double): Outside {
        val j = JSONObject(get(String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,cloud_cover,pressure_msl&hourly=pressure_msl&forecast_hours=24&timezone=auto&timeformat=unixtime", lat, lon)))
        val c = j.getJSONObject("current")
        val h = j.optJSONObject("hourly")?.optJSONArray("pressure_msl")
        val ahead = h?.let { a -> (0 until a.length()).mapNotNull { a.optDouble(it).takeIf { v -> !v.isNaN() } } } ?: emptyList()
        return Outside(ofCode(c.optInt("weather_code"), c.optInt("cloud_cover", 50)), c.optDouble("temperature_2m"), c.optDouble("pressure_msl"), ahead, System.currentTimeMillis())
    }

    private fun metNo(lat: Double, lon: Double): Outside {
        val j = JSONObject(get(String.format(Locale.US, "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.3f&lon=%.3f", lat, lon)))
        val ts = j.getJSONObject("properties").getJSONArray("timeseries")
        val first = ts.getJSONObject(0).getJSONObject("data")
        val inst = first.getJSONObject("instant").getJSONObject("details")
        val cloud = inst.optDouble("cloud_area_fraction", 50.0).toInt()
        val sym = first.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code").orEmpty()
        val w = when {
            "thunder" in sym -> Weather.STORM
            "snow" in sym || "sleet" in sym -> Weather.SNOW
            "rain" in sym -> Weather.RAIN
            "fog" in sym -> Weather.FOG
            "clearsky" in sym || "fair" in sym -> Weather.CLEAR
            cloud > 85 -> Weather.OVERCAST
            cloud > 30 -> Weather.CLOUDY
            else -> Weather.CLEAR
        }
        val ahead = (1 until minOf(25, ts.length())).mapNotNull { i ->
            ts.getJSONObject(i).getJSONObject("data").getJSONObject("instant").getJSONObject("details").optDouble("air_pressure_at_sea_level").takeIf { !it.isNaN() } }
        return Outside(w, inst.optDouble("air_temperature"), inst.optDouble("air_pressure_at_sea_level"), ahead, System.currentTimeMillis())
    }

    fun towns(q: String, ru: Boolean): List<Town> {
        val j = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?count=8&language=${if (ru) "ru" else "en"}&name=" + URLEncoder.encode(q, "UTF-8")))
        val a = j.optJSONArray("results") ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
            Town(o.optString("name"), listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "), o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    /** A first guess at the town, from the phone's time zone («Europe/Moscow» → Moscow). */
    fun guess(ru: Boolean, zone: ZoneId = ZoneId.systemDefault()): Town? {
        val city = zone.id.substringAfterLast('/').replace('_', ' ')
        if (city.isBlank() || zone.id.startsWith("Etc") || zone.id == "UTC") return null
        return runCatching { towns(city, ru).firstOrNull() }.getOrNull()
    }
}
