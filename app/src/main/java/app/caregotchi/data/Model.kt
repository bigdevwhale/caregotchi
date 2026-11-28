package app.caregotchi.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * One thing done for yourself. [kind]: eat, water, pill, sleep (went to bed), wake, move.
 * eat: [sub] is breakfast / lunch / dinner / snack; [q] how wholesome (0 not said, 1 fast food or sweets, 2 ordinary, 3 wholesome);
 * [tags] what was in it (veg, fruit, protein, carbs – porridge, pasta, bread (#14) –, sweet). move: [sub] walk / run / gym / yoga / bike / swim / dance / other, [mins] how long.
 * bp: a blood pressure reading – [sys]/[dia] mmHg and [pulse].
 * pill: [med] is the medicine, [slot] the scheduled minute of the day it answers (-1 when taken as needed), [late] minutes after the slot.
 */
@Entity(indices = [Index("at")])
data class Mark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val kind: String,
    val sub: String = "",
    val med: Long = 0,
    val slot: Int = -1,
    val late: Int = 0,
    val q: Int = 0,
    val tags: String = "",
    val mins: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "0") val sys: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "0") val dia: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "0") val pulse: Int = 0,
    val xp: Int = 0,
    val leaves: Int = 0,
)

/**
 * A medicine. [times] are minutes of the day, comma-separated («480,1200»); empty for as-needed.
 * [pain]: 0 not a painkiller, 1 a simple one (paracetamol, ibuprofen: 15 days a month is the line),
 * 2 a triptan or a combination (10 days). [stock] pills left and [perDose] pills a dose, both in halves of a pill (#9),
 * stock 0 when not counted. [pattern]: halves a dose by day of a repeating cycle from [from] («2,2,1» is 1 / 1 / ½;
 * 0 a day off), empty when every day is [perDose]. [until] the course's
 * last epoch day, 0 when there is no end; [from] its first day (a course can be planned ahead). [herb] is the pot it grows on the windowsill.
 * [food]: 0 not tied to meals, 1 before a meal, 2 with it, 3 after it; [gap] minutes before or after; [meals] which of the meal
 * times (indices into the settings' meal times, comma-separated) – then [times] are worked out from those meal times.
 */
@Entity
data class Med(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String = "",
    val times: String = "",
    val pain: Int = 0,
    val stock: Int = 0,
    val perDose: Int = 2,
    val from: Long = 0,
    val until: Long = 0,
    val herb: String = "mint",
    val archived: Boolean = false,
    val created: Long = 0,
    @androidx.room.ColumnInfo(defaultValue = "0") val food: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "30") val gap: Int = 30,
    @androidx.room.ColumnInfo(defaultValue = "") val meals: String = "",
    @androidx.room.ColumnInfo(defaultValue = "") val pattern: String = "",
    /**
     * Earlier schedules (#28), «day:480;day:480,1200»: each one's [times] held on the days before its day (and from the
     * previous entry's day). A change of times counts from the day it was made, so the past keeps the doses it had.
     */
    @androidx.room.ColumnInfo(defaultValue = "") val past: String = "",
) {
    companion object {
        /** Whole days [stock] halves of [m] last from [day] at its schedule and cycle; null when it takes none. */
        fun daysOf(m: Med, stock: Int, day: Long): Int? {
            val n = m.timeList().size
            if (n == 0 || (0 until maxOf(1, m.patternList().size)).all { m.halvesOn(day + it) == 0 }) return null
            var left = stock; var d = 0
            while (d < 3650) { val need = n * m.halvesOn(day + d); if (need > left) break; left -= need; d++ }
            return d
        }
    }

    fun timeList(): List<Int> = times.split(',').mapNotNull { it.trim().toIntOrNull() }.sorted()

    private fun pastList(): List<Pair<Long, List<Int>>> = past.split(';').mapNotNull { e ->
        val d = e.substringBefore(':', "").trim().toLongOrNull() ?: return@mapNotNull null
        d to e.substringAfter(':').split(',').mapNotNull { it.trim().toIntOrNull() }.sorted()
    }.sortedBy { it.first }

    /** The times the schedule had on [day] (#28): an earlier one for days before a change, else today's. */
    fun timesOn(day: Long): List<Int> = pastList().firstOrNull { day < it.first }?.second ?: timeList()

    /**
     * One repair for schedules changed before 1.4.5 (#28): a dose first marked three or more days after the medicine's
     * first marked dose was added later, so the days before it don't ask for it. [firstBySlot]: each time's first marked day.
     */
    fun guessPast(firstBySlot: Map<Int, Long>): Med {
        if (past.isNotBlank() || timeList().size < 2) return this
        val t = timeList(); val base = firstBySlot.values.minOrNull() ?: return this
        val later = t.filter { (firstBySlot[it] ?: return this) >= base + 3 }
        if (later.isEmpty() || later.size == t.size) return this
        val days = later.map { firstBySlot.getValue(it) }.distinct().sorted()
        return copy(past = days.joinToString(";") { d -> "$d:" + t.filter { (firstBySlot[it] ?: Long.MAX_VALUE) < d }.joinToString(",") })
    }

    /** This medicine with new [times] from [today] on: the old ones stay for the days before (once a day: the day's first). */
    fun withTimes(times: String, today: Long): Med {
        val new = times.split(',').mapNotNull { it.trim().toIntOrNull() }.sorted()
        if (new == timeList() || id == 0L) return copy(times = times)
        val keep = pastList().filter { it.first < today }
        val prev = pastList().firstOrNull { it.first == today }?.second ?: timeList()
        val hist = keep + (today to prev)
        return copy(times = times, past = hist.joinToString(";") { (d, t) -> "$d:" + t.joinToString(",") })
    }
    val asNeeded get() = times.isBlank()
    fun patternList(): List<Int> = pattern.split(',').mapNotNull { it.trim().toIntOrNull() }
    /** Which day of the cycle [from] was («2,2,1,@1»: the course began on its second day), so the days keep the numbers the user gave them. */
    fun cycleStart(): Int = pattern.substringAfter('@', "").trim().toIntOrNull() ?: 0
    /** Halves of a pill a dose on [day]: the cycle's day counted from the first day, or [perDose]. 0 is a day off. */
    fun halvesOn(day: Long): Int = patternList().let { p -> if (p.size < 2) p.firstOrNull() ?: perDose else p[Math.floorMod(day - from + cycleStart(), p.size.toLong()).toInt()] }
    /** Halves a pill taken on [day] uses up: that day's dose, or the usual one when it was a day off (taken anyway). */
    fun taken(day: Long): Int = halvesOn(day).takeIf { it > 0 } ?: perDose.coerceAtLeast(1)
    fun mealList(): List<Int> = meals.split(',').mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()
    /** The times when tied to meals: each chosen meal time, moved by the gap. */
    fun timesFor(mealTimes: List<Int>): List<Int> = mealList().mapNotNull { mealTimes.getOrNull(it) }
        .map { ((it + when (food) { 1 -> -gap; 3 -> gap; else -> 0 }) % 1440 + 1440) % 1440 }.distinct().sorted()
}

/**
 * A bad spell: a migraine, a headache, a stomach, anything. [end] is 0 while it lasts.
 * [strength] 1..10 (0 not said). [symptoms], [triggers] and [took] (medicine ids or free names) are comma lists.
 * [relief]: 0 not asked yet, 1 no, 2 a little, 3 yes, after what was taken.
 */
@Entity(indices = [Index("start")])
data class Attack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val start: Long,
    val end: Long = 0,
    val kind: String = "migraine",
    val strength: Int = 0,
    val side: String = "",
    val symptoms: String = "",
    val triggers: String = "",
    val took: String = "",
    val relief: Int = 0,
    val note: String = "",
    /** How the pain went (#11): «ms:strength» pairs, oldest first; [strength] is then the worst of them. */
    @androidx.room.ColumnInfo(defaultValue = "") val curve: String = "",
) {
    fun list(s: String) = s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    /** The points of [curve]. */
    val points: List<Pair<Long, Int>> get() = curve.split(',').mapNotNull { p -> p.split(':').takeIf { it.size == 2 }?.let { (t, v) -> t.toLongOrNull()?.let { tt -> v.toIntOrNull()?.let { tt to it } } } }
    /** How strong it is now: the last point, or the one strength said. */
    val now: Int get() = points.lastOrNull()?.second ?: strength
    /** The pain changed to [v] at [at]: a point on the curve (a change within two minutes corrects the last one). */
    fun felt(v: Int, at: Long = System.currentTimeMillis()): Attack {
        val ps = points.toMutableList()
        if (ps.isNotEmpty() && at - ps.last().first < 120_000) ps.removeAt(ps.lastIndex)
        // the first word on a fresh spell belongs to its start
        val t = if (ps.isEmpty() && at - start < 15 * 60_000) start else at
        ps += t to v
        return copy(curve = ps.joinToString(",") { "${it.first}:${it.second}" }, strength = ps.maxOf { it.second })
    }
}

/** A pet of the hollow. Until [hatched] it is an egg. [xp] is the care it has shared. [home] is the one in the hollow now. */
@Entity
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val name: String,
    val found: Long,
    val hatched: Long = 0,
    val xp: Int = 0,
    val home: Boolean = false,
)

/** The air pressure seen on a day, hPa, from the weather (or the phone's barometer). */
@Entity
data class Air(
    @PrimaryKey val day: Long,
    val min: Double,
    val max: Double,
    val last: Double,
)

@Dao
interface Dao0 {
    @Query("SELECT * FROM Mark ORDER BY at DESC") fun marks(): Flow<List<Mark>>
    @Query("SELECT * FROM Mark ORDER BY at DESC") suspend fun markList(): List<Mark>
    @Query("SELECT * FROM Mark WHERE at >= :from ORDER BY at DESC") suspend fun marksSince(from: Long): List<Mark>
    @Insert suspend fun insert(m: Mark): Long
    @Insert suspend fun insertMarks(m: List<Mark>)
    @Update suspend fun update(m: Mark)
    @Query("DELETE FROM Mark WHERE id = :id") suspend fun deleteMark(id: Long)

    @Query("SELECT * FROM Med ORDER BY created, id") fun meds(): Flow<List<Med>>
    @Query("SELECT * FROM Med ORDER BY created, id") suspend fun medList(): List<Med>
    @Insert suspend fun insert(m: Med): Long
    @Update suspend fun update(m: Med)

    @Query("SELECT * FROM Attack ORDER BY start DESC") fun attacks(): Flow<List<Attack>>
    @Query("SELECT * FROM Attack ORDER BY start DESC") suspend fun attackList(): List<Attack>
    @Query("SELECT * FROM Attack WHERE id = :id") suspend fun attack(id: Long): Attack?
    @Insert suspend fun insert(a: Attack): Long
    @Insert suspend fun insertAttacks(a: List<Attack>)
    @Update suspend fun update(a: Attack)
    @Query("DELETE FROM Attack WHERE id = :id") suspend fun deleteAttack(id: Long)

    @Query("SELECT * FROM Pet ORDER BY found, id") fun pets(): Flow<List<Pet>>
    @Query("SELECT * FROM Pet ORDER BY found, id") suspend fun petList(): List<Pet>
    @Insert suspend fun insert(p: Pet): Long
    @Update suspend fun update(p: Pet)
    @Update suspend fun updatePets(p: List<Pet>)

    @Query("SELECT * FROM Air ORDER BY day DESC") fun air(): Flow<List<Air>>
    @Query("SELECT * FROM Air ORDER BY day DESC") suspend fun airList(): List<Air>
    @Query("SELECT * FROM Air WHERE day = :day") suspend fun air(day: Long): Air?
    @androidx.room.Upsert suspend fun upsert(a: Air)
    @androidx.room.Upsert suspend fun upsertAir(a: List<Air>)

    @Insert suspend fun insertMeds(m: List<Med>)
    @Insert suspend fun insertPets(p: List<Pet>)
    @Query("DELETE FROM Mark") suspend fun clearMarks()
    @Query("DELETE FROM Med") suspend fun clearMeds()
    @Query("DELETE FROM Attack") suspend fun clearAttacks()
    @Query("DELETE FROM Pet") suspend fun clearPets()
    @Query("DELETE FROM Air") suspend fun clearAir()
}

@Database(entities = [Mark::class, Med::class, Attack::class, Pet::class, Air::class], version = 6, exportSchema = true,
    autoMigrations = [androidx.room.AutoMigration(from = 1, to = 2), androidx.room.AutoMigration(from = 2, to = 3), androidx.room.AutoMigration(from = 4, to = 5), androidx.room.AutoMigration(from = 5, to = 6)])
abstract class Db : RoomDatabase() {
    abstract fun dao(): Dao0

    companion object {
        /** Pills are counted in halves from here on (#9), and a dose can change by the day of a cycle. */
        val M3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Med` ADD COLUMN `pattern` TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE `Med` SET `stock` = `stock` * 2, `perDose` = `perDose` * 2")
            }
        }

        fun create(ctx: Context) = Room.databaseBuilder(ctx, Db::class.java, "caregotchi.db").addMigrations(M3_4).fallbackToDestructiveMigration(true).build()
    }
}
