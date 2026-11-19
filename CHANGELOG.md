# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
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
