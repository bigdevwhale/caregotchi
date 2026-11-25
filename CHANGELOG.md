# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.4.3] - 2026-11-25

### Added
- **«Carbs» on the plate (#14):** porridge, pasta, bread – a fifth thing to tick after a meal, beside vegetables, fruit, protein and sweets.
- **Tomorrow's tasks (#23):** the tasks window shows tomorrow's three under today's, so you can get ready for them.

### Changed
- **«Up on time» keeps to your own morning (#23):** the task asks to be up by half an hour after the wake-up time in the settings, not by eight for everyone.
- **The Sunday letter speaks of your breakfasts (#24):** what was most often in them – porridge and bread, protein, vegetables – instead of a porridge the pet made up.

### Fixed
- **Words agree with numbers (#24):** «4 хороших дня» in the letter, «1 хороший» on the quilt, «ещё один хороший день» on the egg.

<!-- tg
В еде появились «Углеводы» – каша, макароны, хлеб
В заданиях видно, что будет завтра, а «встать вовремя» считается от вашего времени подъёма
Письмо питомца теперь рассказывает о ваших завтраках и пишет «4 хороших дня» без ошибок
-->

## [1.4.2] - 2026-11-23

### Fixed
- **A medicine's page no longer closes the app (#21):** a course that began before today, with the pills counted and the same dose every day, crashed on opening its pot.
- **A dose missed in the morning isn't asked for all day (#21):** three hours after its time the pet stops calling for it, and «Pill» on the widget notes the dose nearest now, not that morning one.
- **«Pill» lets you note several medicines (#22):** «Taken» no longer closes the list – a dose taken gets a tick with its time, and «Done» closes it.
- **The leaves count is seen by candlelight (#16):** on the home screen and in the shop it was light on light paper.
- **The fairy lights hang over the window (#17):** they were drawn a fifth of their size behind the top bar. The hammock and the rocking chair now keep their full size too.

<!-- tg
Исправили вылет при нажатии на растение на подоконнике
Пропущенное утром лекарство больше не требуется весь день
В «Таблетке» можно отметить несколько лекарств подряд – список закрывается кнопкой «Готово»
Листики видно в теме «При свече», а гирлянда висит над окном во всю ширину
-->

## [1.4.1] - 2026-11-22

### Fixed
- **Meals are named by your own times (#19):** with meals at 11:30 and 14:00 both were called lunch, so the 11:30 reminder spoke of lunch. Now the first meal is breakfast, the last dinner, the one nearest midday lunch and the rest snacks – in reminders, settings and the diary.
- **«Pill» always shows the list from the sill (#20):** you pick what you took, with the doses due now on top. Before, with a dose due, the button noted the first one at once.

<!-- tg
Напоминания о еде теперь называются по вашему расписанию: если обед в 14:00, в 11:30 придёт напоминание о перекусе, а не об обеде
Кнопка «Таблетка» всегда открывает список лекарств с подоконника – отмечаете то, что приняли
-->

## [1.4.0] - 2026-11-22

### Added
- **How the pain went (#11):** change the strength while a spell lasts and each change is kept with its time – «8:00 – 3 → 9:10 – 5 → 10:00 – 3 · eased at 11:00». The line is in «I feel bad», in the diary and in the report for the doctor; the spell's strength is its worst point.
- **What you took, in your own words (#11):** «+ Other» under «Took» for something that isn't on the sill.

### Changed
- **The widget fills its space:** the pet with its name and state on top, five buttons across the whole width below, and it can be stretched in height.
- **Food on the widget opens the app** and is noted there, with «How did you eat?» at once. Water, a pill and sleep are still noted on the widget itself.

### Fixed
- **A press on the widget is seen to work (#13):** the widget is redrawn before the press is let go – before, the system could freeze the app first, the sleep button looked untouched and a second press undid it. While the pet sleeps the widget says «✓ Asleep since …» with the button lit, and water or a pill just noted says so too. An accidental double tap no longer undoes the first one.

<!-- tg
Виджет теперь показывает, что нажатие сработало: «✓ Спит с …», «✓ Вода записана», – и занимает всё своё место; еда открывает приложение с вопросом «Как поел?»
При приступе можно менять силу – время каждого изменения сохранится и попадёт в отчёт для врача
В «Принял» появилось «+ Другое» для того, чего нет на подоконнике
-->

## [1.3.1] - 2026-11-20

### Fixed
- «Today is day N» on a medicine with a dose that changes by the day was lost: for a course that began today it went back to day 1, and for a running one the days got new numbers. The cycle is now kept in the order you gave it, with the day of it the course began on. (#10)

<!-- tg
Исправили разную дозу по дням: выбранный «сегодня – день 2» больше не слетает на первый день.
-->

## [1.3.0] - 2026-11-20

### Added
- **Put a record right, or add one afterwards.** Tap any mark on the day's thread or in a need's sheet to change its day and time and what it was – the meal and how wholesome, the sport and minutes, the drink and milligrams, the blood pressure, the medicine and its scheduled time – or to delete it. «Add something earlier» (in the sheets and «+ Add to this day» on a day's page) asks when first, then what, and opens the new record to fill in. A bad spell's start can now move to another day too. (#7)
- **Your own meal times, and medicine tied to them.** Settings → Reminders lists the meals with − and + by a quarter of an hour, a cross to drop one and «+ one more meal» (up to six); the meal reminder comes at those times when nothing was eaten yet. A medicine can be taken before a meal, with it or after it, 15 minutes to 2 hours, at the meals you pick: its times follow the meal times, and an after-meal reminder moves along when the meal was later. (#6)
- **A course planned ahead.** A new medicine can start today, tomorrow, next Monday, on the 1st or any day; the course length counts from then. Until it starts its pot stands pale with the date, a slip says «Starting soon», and the evening before the pet writes ««…» starts tomorrow». (#5)
- **The period as a state, not a spell.** With periods on, «I feel bad» has its own row: it began today, yesterday or the day before, it ended, the cycle day and the next one expected, the average cycle. Symptoms – a headache, the stomach, the back, anxiety and the new **insomnia** – are written apart, with their start and end. The detective shows which of them keep to a part of the cycle (the three days before, the first day, the days of it) and how long they usually last, and whether spells come more often around the period; a spell then gets «Cycle» as a likely trigger. The quilt marks the period's days with a rose bead and the next expected one with a ring. The period no longer counts among the spells, on the home screen or in the doctor's report. (#8)
- **Half pills, a course of any length, a dose that changes by the day.** The dose and the stock go by halves: ½, 1, 1½. Besides 7–90 days, «Other» sets any length, 20 days or 45. «Different by day» gives a cycle of up to 14 days (1 / 1 / ½, or a day off) that repeats from the first day; for a running medicine you say which day today is. Reminders, the pill sheet and the doctor's report say the day's dose, and «lasts N days» follows the cycle. (#9)

### Fixed
- The pots on a medicine's page, and other picture tiles, stood at different heights when a name took two lines; they now keep one height in a row, and the pots are three to a row.

<!-- tg
Записи можно исправлять: время, день и что именно было. А забытое – вносить задним числом.
Своё время еды для тех, кто ест по часам, и лекарства «за 30 минут до еды» или «через час после»: напоминание сдвинется следом за обедом.
Курс лекарства можно запланировать заранее, например с 1 декабря, – накануне питомец напомнит.
Месячные теперь не приступ, а состояние: начало, конец и день цикла. Симптомы пишутся отдельно, и сыщик покажет, в какие дни цикла они приходят.
Можно принимать по половинке таблетки, задать курс любой длины, например 20 дней, и разную дозу по дням: 1, 1, ½ и снова по кругу.
-->

## [1.2.0] - 2026-11-19

### Added
- **Caffeine.** A «Coffee» button next to «I feel bad» opens drawn drinks – espresso, coffee, latte, cappuccino, black and green tea, an energy drink, cola – each with its usual milligrams, and a double. The button shows today's milligrams; the sheet shows them against 400. After four the pet mentions sleep, and over 400 it suggests water. The detective now checks days with 300 mg or more and, for coffee drinkers, days without any (withdrawal headaches); a spell on a heavy coffee day gets «Coffee» as a likely trigger by itself. Drinks show on the day's thread. (#4)

<!-- tg
Появился кофеин: кнопка «Кофе» рядом с «Мне плохо» – эспрессо, латте, чай, энергетик и другие напитки, у каждого свои миллиграммы.
Питомец показывает, сколько кофеина набралось за день, а сыщик проверяет, не связаны ли приступы с лишним кофе или с днями совсем без него.
-->

## [1.1.0] - 2026-11-19

### Added
- **Blood pressure, morning and evening.** A reminder at two times you choose (8:00 and 20:00 by default) with a «Write it down» button, and a sheet for the upper and lower numbers and the pulse, filled with the last reading. Readings show on the day's thread and go into the doctor's PDF with morning and evening averages. «I feel bad» → «Blood pressure» opens the same sheet. (#2)
- **A bed / wake-up button on the widget.** It shows a moon in the evening and a sun in the morning, like the one in the hollow. (#3)

### Fixed
- **«Allow notifications» did nothing.** The app now shows whether notifications are really on. When they are off, the button asks Android, and when Android no longer shows its question, it opens the app's notification settings. A warning appears in Settings while notifications are off. (#1)
- **A meal rating, a workout or a pill picked in a sheet could be lost** when the sheet closed before it was saved.

<!-- tg
Можно записывать давление: утром и вечером питомец напомнит измерить, а в шторке остаётся ввести верхнее, нижнее и пульс. Измерения видны в дневнике и попадают в отчёт для врача.
На виджете появилась кнопка «Отбой / Подъём».
Кнопка «Разрешить уведомления» теперь работает: если Android больше не спрашивает, откроются настройки уведомлений.
Исправлено: оценка еды, тренировка или таблетка из шторки иногда не сохранялись.
-->

## [1.0.0] - 2026-11-19

First release.

- A pet in a tree hollow that lives on your self-care: meals (and how wholesome they were), water, medicine, moving and sleep.
- «I feel bad»: migraine and other troubles; a dimmed dark room for headaches, a drawn head to show where it hurts, «Feeling better?» two hours after medicine.
- Medicine as pots on the windowsill, reminders with a «Taken» button, a weekly pill box, the stock and a count of painkiller days.
- The detective: links between spells and sleep, meals, water, moving, medicine and air pressure.
- The diary: a patchwork quilt for the month, a page for each day, the pet's Sunday letters, a PDF report for the doctor.
- 12 pets, levels, leaves, the Badger's shop, daily tasks, a weekly chest, 16 badges, a home-screen widget.
- A backup file to move to another phone. Russian and English.

<!-- tg
«Заботогочи» – тамагочи, которого кормит ваша забота о себе: еда, вода, лекарства, движение и сон.
Можно записывать мигрень и другие болячки. Питомец от этого не страдает, а сыщик ищет, что их вызывает.
Лекарства растут горшками на подоконнике, по воскресеньям приходит письмо от питомца, а врачу можно отправить отчёт в PDF.
-->
