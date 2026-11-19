package app.caregotchi.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import app.caregotchi.container
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything in one file, to move to another phone: the settings (leaves, level, things bought, badges),
 * the marks, the medicines, the bad spells, the pets and the pressure days. Restoring replaces what is here.
 */
object Backup {
    private const val FORMAT = "caregotchi-backup"

    suspend fun write(ctx: Context, uri: Uri) {
        val c = ctx.container
        val j = JSONObject().put("format", FORMAT).put("version", 1).put("at", System.currentTimeMillis())
            .put("settings", c.settings.now().json())
            .put("marks", JSONArray(c.dao.markList().map { m -> JSONObject().put("at", m.at).put("kind", m.kind).put("sub", m.sub).put("med", m.med).put("slot", m.slot)
                .put("late", m.late).put("q", m.q).put("tags", m.tags).put("mins", m.mins).put("sys", m.sys).put("dia", m.dia).put("pulse", m.pulse).put("xp", m.xp).put("leaves", m.leaves) }))
            .put("meds", JSONArray(c.dao.medList().map { m -> JSONObject().put("id", m.id).put("name", m.name).put("dose", m.dose).put("times", m.times).put("pain", m.pain)
                .put("stock", m.stock).put("perDose", m.perDose).put("from", m.from).put("until", m.until).put("herb", m.herb).put("archived", m.archived).put("created", m.created).put("food", m.food).put("gap", m.gap).put("meals", m.meals) }))
            .put("attacks", JSONArray(c.dao.attackList().map { a -> JSONObject().put("start", a.start).put("end", a.end).put("kind", a.kind).put("strength", a.strength).put("side", a.side)
                .put("symptoms", a.symptoms).put("triggers", a.triggers).put("took", a.took).put("relief", a.relief).put("note", a.note) }))
            .put("pets", JSONArray(c.dao.petList().map { p -> JSONObject().put("species", p.species).put("name", p.name).put("found", p.found).put("hatched", p.hatched).put("xp", p.xp).put("home", p.home) }))
            .put("air", JSONArray(c.dao.airList().map { a -> JSONObject().put("day", a.day).put("min", a.min).put("max", a.max).put("last", a.last) }))
        ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(j.toString().toByteArray()) }
    }

    /** Reads a backup; returns false when the file isn't one. */
    suspend fun read(ctx: Context, uri: Uri): Boolean {
        val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return false
        val j = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (j.optString("format") != FORMAT) return false
        fun arr(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
        val c = ctx.container
        val meds = arr("meds").map { o -> Med(id = o.getLong("id"), name = o.getString("name"), dose = o.optString("dose"), times = o.optString("times"), pain = o.optInt("pain"),
            stock = o.optInt("stock"), perDose = o.optInt("perDose", 1), from = o.optLong("from"), until = o.optLong("until"), herb = o.optString("herb", "mint"),
            archived = o.optBoolean("archived"), created = o.optLong("created"), food = o.optInt("food"), gap = o.optInt("gap", 30), meals = o.optString("meals")) }
        val marks = arr("marks").map { o -> Mark(at = o.getLong("at"), kind = o.getString("kind"), sub = o.optString("sub"), med = o.optLong("med"), slot = o.optInt("slot", -1),
            late = o.optInt("late"), q = o.optInt("q"), tags = o.optString("tags"), mins = o.optInt("mins"), sys = o.optInt("sys"), dia = o.optInt("dia"), pulse = o.optInt("pulse"), xp = o.optInt("xp"), leaves = o.optInt("leaves")) }
        val attacks = arr("attacks").map { o -> Attack(start = o.getLong("start"), end = o.optLong("end"), kind = o.optString("kind", "migraine"), strength = o.optInt("strength"),
            side = o.optString("side"), symptoms = o.optString("symptoms"), triggers = o.optString("triggers"), took = o.optString("took"), relief = o.optInt("relief"), note = o.optString("note")) }
        val pets = arr("pets").map { o -> Pet(species = o.getString("species"), name = o.getString("name"), found = o.optLong("found"), hatched = o.optLong("hatched"), xp = o.optInt("xp"), home = o.optBoolean("home")) }
        val air = arr("air").map { o -> Air(o.getLong("day"), o.getDouble("min"), o.getDouble("max"), o.getDouble("last")) }
        val settings = j.optJSONObject("settings")?.let { Settings.of(it) } ?: return false
        c.db.withTransaction {
            c.dao.clearMarks(); c.dao.clearMeds(); c.dao.clearAttacks(); c.dao.clearPets(); c.dao.clearAir()
            c.dao.insertMeds(meds); c.dao.insertMarks(marks); c.dao.insertAttacks(attacks); c.dao.insertPets(pets); c.dao.upsertAir(air)
        }
        c.settings.update { settings.copy(onboarded = true, inbox = emptyList()) }
        return true
    }
}
