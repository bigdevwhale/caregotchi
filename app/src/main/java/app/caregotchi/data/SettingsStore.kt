package app.caregotchi.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Everything that isn't a mark, a medicine, a bad spell or a pet: leaves, the hollow's things, the day's tasks. */
data class Settings(
    val onboarded: Boolean = false,
    val leaves: Int = 0,                         // the money
    val leavesEver: Int = 0,
    val xp: Int = 0,                             // the keeper's experience; the level comes from it
    val items: Set<String> = emptySet(),         // things bought for the hollow
    val hidden: Set<String> = emptySet(),        // bought things put away
    val plaids: Int = 1,                         // blankets that keep a missed day from breaking the streak
    val plaidDays: Set<Long> = emptySet(),       // days under the blanket: needs stand still, the day counts as good
    val questDay: Long = 0,
    val quests: List<String> = emptyList(),
    val questsDone: Set<String> = emptySet(),
    val chestWeek: Long = 0,                     // the Monday of the last chest opened
    val badges: Set<String> = emptySet(),
    val eggsSeen: Set<String> = emptySet(),      // species whose egg has been given
    val inbox: List<String> = emptyList(),       // news to show once: «egg:owl», «hatch:12», «level:8», «badge:3», «stage:12:2»
    val seen: Set<String> = emptySet(),          // hints read
    val clues: Map<String, Int> = emptyMap(),    // clue id → 1 kept, -1 struck
    val theme: String = "system",
    val fem: Boolean = false,                    // Russian forms: «поел» / «поела»
    val strict: Boolean = false,                 // the pet may leave for grandma's after a week of no care
    val waterGoal: Int = 6,                      // glasses a day
    val sleepGoal: Int = 8,                      // hours
    val moveGoal: Int = 30,                      // minutes of moving a day
    val bedtime: Int = 23 * 60,                  // minutes of the day
    val wake: Int = 7 * 60 + 30,
    val remindMeds: Boolean = true,
    val remindMeals: Boolean = false,
    val remindWater: Boolean = false,
    val remindBed: Boolean = false,
    val remindBp: Boolean = false,               // measure the blood pressure
    val bpTimes: List<Int> = listOf(8 * 60, 20 * 60), // minutes of the day for it
    val reliefAsk: Boolean = true,               // ask «better?» two hours after a bad spell
    val pressureWarn: Boolean = true,
    val cycle: Boolean = false,                  // mark the period among the troubles
    val kinds: List<String> = emptyList(),       // own kinds of trouble, besides the built-in ones
    val town: String = "", val lat: Double = Double.NaN, val lon: Double = Double.NaN,
    val guessed: Boolean = false,
    val outside: String = "",
    val warnedDay: Long = 0,                     // the last day the pressure warning was shown
    val letterWeek: Long = 0,                    // the Monday of the last letter read
) {
    fun json(): JSONObject = JSONObject().apply {
        fun arr(c: Collection<Any>) = JSONArray(c.toList())
        put("onboarded", onboarded); put("leaves", leaves); put("leavesEver", leavesEver); put("xp", xp); put("items", arr(items)); put("hidden", arr(hidden))
        put("plaids", plaids); put("plaidDays", arr(plaidDays)); put("questDay", questDay); put("quests", arr(quests)); put("questsDone", arr(questsDone))
        put("chestWeek", chestWeek); put("badges", arr(badges)); put("eggsSeen", arr(eggsSeen)); put("inbox", arr(inbox)); put("seen", arr(seen))
        put("clues", JSONObject(clues as Map<*, *>)); put("theme", theme); put("fem", fem); put("strict", strict); put("waterGoal", waterGoal); put("sleepGoal", sleepGoal); put("moveGoal", moveGoal)
        put("bedtime", bedtime); put("wake", wake); put("remindMeds", remindMeds); put("remindMeals", remindMeals); put("remindWater", remindWater); put("remindBed", remindBed); put("remindBp", remindBp); put("bpTimes", arr(bpTimes))
        put("reliefAsk", reliefAsk); put("pressureWarn", pressureWarn); put("cycle", cycle); put("kinds", arr(kinds))
        put("town", town); if (!lat.isNaN()) { put("lat", lat); put("lon", lon) }; put("guessed", guessed); put("outside", outside); put("warnedDay", warnedDay); put("letterWeek", letterWeek)
    }

    companion object {
        fun of(j: JSONObject): Settings {
            fun list(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            fun set(k: String) = list(k).toSet()
            val d = Settings()
            val cl = j.optJSONObject("clues")?.let { o -> o.keys().asSequence().associateWith { o.optInt(it) } } ?: emptyMap()
            return Settings(
                onboarded = j.optBoolean("onboarded"), leaves = j.optInt("leaves"), leavesEver = j.optInt("leavesEver"), xp = j.optInt("xp"), items = set("items"), hidden = set("hidden"),
                plaids = j.optInt("plaids", d.plaids), plaidDays = list("plaidDays").mapNotNull { it.toLongOrNull() }.toSet(), questDay = j.optLong("questDay"), quests = list("quests"),
                questsDone = set("questsDone"), chestWeek = j.optLong("chestWeek"), badges = set("badges"), eggsSeen = set("eggsSeen"), inbox = list("inbox"), seen = set("seen"),
                clues = cl, theme = j.optString("theme", d.theme), fem = j.optBoolean("fem"), strict = j.optBoolean("strict"), waterGoal = j.optInt("waterGoal", d.waterGoal),
                sleepGoal = j.optInt("sleepGoal", d.sleepGoal), moveGoal = j.optInt("moveGoal", d.moveGoal), bedtime = j.optInt("bedtime", d.bedtime), wake = j.optInt("wake", d.wake),
                remindMeds = j.optBoolean("remindMeds", true), remindMeals = j.optBoolean("remindMeals"), remindWater = j.optBoolean("remindWater"), remindBed = j.optBoolean("remindBed"), remindBp = j.optBoolean("remindBp"), bpTimes = if (j.has("bpTimes")) list("bpTimes").mapNotNull { it.toIntOrNull() } else d.bpTimes,
                reliefAsk = j.optBoolean("reliefAsk", true), pressureWarn = j.optBoolean("pressureWarn", true), cycle = j.optBoolean("cycle"), kinds = list("kinds"),
                town = j.optString("town"), lat = j.optDouble("lat", Double.NaN), lon = j.optDouble("lon", Double.NaN), guessed = j.optBoolean("guessed"), outside = j.optString("outside"),
                warnedDay = j.optLong("warnedDay"), letterWeek = j.optLong("letterWeek"))
        }
    }
}

private val Context.store by preferencesDataStore("caregotchi")
private val KEY = stringPreferencesKey("settings")

class SettingsStore(private val ctx: Context) {
    val settings: Flow<Settings> = ctx.store.data.map { p -> p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings() }
    suspend fun now(): Settings = settings.first()
    suspend fun update(f: (Settings) -> Settings) { ctx.store.edit { p ->
        val cur = p[KEY]?.let { runCatching { Settings.of(JSONObject(it)) }.getOrNull() } ?: Settings()
        p[KEY] = f(cur).json().toString() } }
}
