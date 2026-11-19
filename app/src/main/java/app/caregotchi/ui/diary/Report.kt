package app.caregotchi.ui.diary

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import app.caregotchi.data.Attack
import app.caregotchi.domain.Troubles
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.dur
import app.caregotchi.domain.fmt1
import app.caregotchi.domain.fmtDay
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.monthName
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.ui.Data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** A plain report for a doctor: three months of spells, painkiller days, medicine taken and what helped. */
object Report {
    suspend fun share(ctx: Context, d: Data) {
        val f = withContext(Dispatchers.IO) { make(ctx, d) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), tr("Отчёт для врача", "Report for the doctor")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun make(ctx: Context, d: Data): File {
        val doc = PdfDocument()
        val W = 595; val H = 842; val M = 40f
        val title = Paint().apply { textSize = 18f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD) }
        val h = Paint().apply { textSize = 12.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val p = Paint().apply { textSize = 10f; typeface = Typeface.SANS_SERIF }
        val grey = Paint(p).apply { color = 0xFF777777.toInt() }
        val red = Paint().apply { color = 0xFFA33B2C.toInt() }
        val box = Paint().apply { style = Paint.Style.STROKE; color = 0xFFBBBBBB.toInt(); strokeWidth = .7f }
        var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, 1).create())
        var cv = page.canvas
        var y = M + 10
        var n = 1
        fun newPage() { doc.finishPage(page); n++; page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, n).create()); cv = page.canvas; y = M + 10 }
        fun line(s: String, paint: Paint = p, dy: Float = 15f) { if (y > H - M) newPage(); cv.drawText(s, M, y, paint); y += dy }

        val to = today(); val from = to - 89
        val spells = d.book.spells.filter { dayOf(it.start) in from..to }.sortedBy { it.start }
        line(tr("Дневник самочувствия – Заботогочи", "Wellbeing diary – Caregotchi"), title, 22f)
        line(tr("${fmtDay(from)} – ${fmtDay(to)} ${LocalDate.ofEpochDay(to).year}", "${fmtDay(from)} – ${fmtDay(to)} ${LocalDate.ofEpochDay(to).year}"), grey, 22f)

        line(tr("Сводка", "Summary"), h, 17f)
        val byKind = spells.groupBy { it.kind }
        if (spells.isEmpty()) line(tr("Приступов не записано.", "No spells written down."))
        byKind.forEach { (k, l) ->
            val st = l.filter { it.strength > 0 }.map { it.strength }
            val ln = l.filter { it.end > it.start }.map { (it.end - it.start) / 1000 }
            line("${Troubles.title(k)}: ${l.size}" + tr(" за 90 дней", " in 90 days") + (if (st.isNotEmpty()) tr(", средняя сила ${fmt1(st.average())}/10", ", mean strength ${fmt1(st.average())}/10") else "") +
                (if (ln.isNotEmpty()) tr(", обычно длится ${dur(ln.sorted()[ln.size / 2].toInt())}", ", usually lasts ${dur(ln.sorted()[ln.size / 2].toInt())}") else ""))
        }
        // painkiller days a month
        val months = (from..to).groupBy { LocalDate.ofEpochDay(it).withDayOfMonth(1) }
        months.forEach { (m, days) ->
            val pd = days.count { d.book.care(it)?.painPill == true }
            val sd = days.count { day -> spells.any { dayOf(it.start) == day } }
            line(tr("${monthName(m.monthValue).replaceFirstChar { it.uppercase() }}: дней с приступом $sd, дней с обезболивающими $pd", "${monthName(m.monthValue)}: $sd days with a spell, $pd days with painkillers"))
        }
        y += 6
        // a calendar strip per month
        months.forEach { (m, _) ->
            if (y > H - 90) newPage()
            line(monthName(m.monthValue).replaceFirstChar { it.uppercase() }, h, 14f)
            val len = m.lengthOfMonth(); val cw = (W - 2 * M) / 31f
            for (i in 0 until len) {
                val day = m.toEpochDay() + i
                val x = M + i * cw
                cv.drawRect(x, y - 10, x + cw - 2, y + 8, box)
                cv.drawText("${i + 1}", x + 2, y - 1, Paint(grey).apply { textSize = 7f })
                val sp = spells.filter { dayOf(it.start) == day }
                if (sp.isNotEmpty()) cv.drawCircle(x + cw / 2 - 1, y + 4, 3.2f, red)
                if (d.book.care(day)?.painPill == true) cv.drawRect(x + 1, y + 6, x + cw - 3, y + 8, Paint().apply { color = 0xFFD9A441.toInt() })
            }
            y += 24
        }
        line(tr("● – приступ, жёлтая черта – день с обезболивающими", "● – a spell, yellow bar – a day with painkillers"), grey, 20f)

        line(tr("Лекарства", "Medicine"), h, 17f)
        d.meds.filter { !it.archived || it.until >= from }.forEach { m ->
            val sched = if (m.asNeeded) tr("по необходимости", "as needed") else m.timeList().joinToString(", ") { hhmm(it) }
            var due = 0; var took = 0
            if (!m.asNeeded) for (day in maxOf(from, d.book.first)..to) { val n0 = d.book.dueOn(m, day).size; due += n0; took += minOf(n0, d.book.marksOn(day).count { it.kind == "pill" && it.med == m.id && it.slot >= 0 }) }
            val used = spells.count { "${m.id}" in it.list(it.took) }
            val rated = spells.filter { "${m.id}" in it.list(it.took) && it.relief > 0 }
            line("${m.name} ${m.dose} – $sched" + (if (due > 0) tr(", принято ${took * 100 / due}% доз", ", ${took * 100 / due}% of doses taken") else "") +
                (if (used > 0) tr(", при приступе $used раз", ", for a spell $used times") else "") +
                (if (rated.isNotEmpty()) tr(", помогло ${rated.count { it.relief == 3 }} из ${rated.size}", ", helped ${rated.count { it.relief == 3 }} of ${rated.size}") else ""))
        }
        y += 8
        val bp = d.marks.filter { it.kind == "bp" && dayOf(it.at) in from..to }.sortedByDescending { it.at }
        if (bp.isNotEmpty()) {
            line(tr("Давление", "Blood pressure"), h, 17f)
            fun avg(l: List<app.caregotchi.data.Mark>) = if (l.isEmpty()) "–" else "${l.map { it.sys }.average().toInt()}/${l.map { it.dia }.average().toInt()}" +
                (l.filter { it.pulse > 0 }.takeIf { it.isNotEmpty() }?.let { tr(", пульс ${it.map { m -> m.pulse }.average().toInt()}", ", pulse ${it.map { m -> m.pulse }.average().toInt()}") } ?: "")
            val am = bp.filter { minuteOf(it.at) < 14 * 60 }; val pm = bp.filter { minuteOf(it.at) >= 14 * 60 }
            line(tr("Измерений ${bp.size}. В среднем утром ${avg(am)}, вечером ${avg(pm)}.", "${bp.size} readings. On average ${avg(am)} in the morning, ${avg(pm)} in the evening."))
            line(tr("Дата       Время   Давление   Пульс", "Date       Time    Pressure   Pulse"), grey)
            bp.take(40).forEach { m -> line("${LocalDate.ofEpochDay(dayOf(m.at))}  ${hhmm(minuteOf(m.at)).padStart(5)}   ${"${m.sys}/${m.dia}".padEnd(10)} ${if (m.pulse > 0) m.pulse else "–"}", p, 13f) }
            if (bp.size > 40) line(tr("…и ещё ${bp.size - 40} измерений раньше.", "…and ${bp.size - 40} earlier readings."), grey)
            y += 8
        }
        line(tr("Приступы", "Spells"), h, 17f)
        line(tr("Дата       Время   Длит.      Сила  Что               Принято / помогло", "Date       Time    Length     Str.  What              Taken / helped"), grey)
        spells.reversed().forEach { a -> line(row(a, d), p, 13f) }
        y += 10
        line(tr("Записано в приложении «Заботогочи». Это дневник, а не диагноз.", "Written down in the Caregotchi app. This is a diary, not a diagnosis."), grey)
        doc.finishPage(page)
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "caregotchi-report-${LocalDate.now()}.pdf")
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        return f
    }

    private fun row(a: Attack, d: Data): String {
        val took = a.list(a.took).joinToString(", ") { id -> id.toLongOrNull()?.let { d.book.medById[it]?.name } ?: id }
        val rel = when (a.relief) { 3 -> tr(" – да", " – yes"); 2 -> tr(" – немного", " – a little"); 1 -> tr(" – нет", " – no"); else -> "" }
        return "${LocalDate.ofEpochDay(dayOf(a.start))}  ${hhmm(minuteOf(a.start)).padStart(5)}   ${(if (a.end > 0) dur(((a.end - a.start) / 1000).toInt()) else "–").padEnd(10)} ${(if (a.strength > 0) "${a.strength}" else "–").padEnd(5)} ${Troubles.title(a.kind).take(16).padEnd(17)} ${took.ifEmpty { "–" }}$rel"
    }
}
