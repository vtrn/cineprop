# CineApp — Приложение для кинопроизводства

Это мобильное приложение на базе **Compose Multiplatform**, предназначенное для автоматизации работы съемочной группы (планирование смен, работа со сценарием, трекинг реквизита).

## 🗺 Карта проекта (Структура файлов)

### 📂 База данных (SQLDelight)
*   `composeApp/src/commonMain/sqldelight/org/mosyagin/project/Database.sq` — **Сердце проекта**. Здесь описаны все таблицы (Проекты, Сцены, Актеры, Смены) и SQL-запросы к ним.

### 📂 Экраны (UI Screens)
*Путь: `composeApp/src/commonMain/kotlin/org/mosyagin/project/ui/screens/`*

*   `ProjectListScreen.kt` — Главный экран со списком всех кинопроектов.
*   `ProjectDashboardScreen.kt` — "Пульт управления" конкретным проектом (плитки: Сценарий, Сцены, КПП, Трекер).
*   `ScriptListScreen.kt` — Список загруженных серий сценария.
*   `SceneListScreen.kt` — Список всех сцен проекта с фильтрацией.
*   `SceneDetailScreen.kt` — Детальный просмотр сцены: текст сценария, актеры и реквизит.
*   `KppListScreen.kt` — Управление версиями КПП (Календарно-постановочного плана).
*   `TrackerScreen.kt` — **Трекер смен**. Список всех съемочных дней, взятых из КПП.
*   `ShiftDetailScreen.kt` — Детали конкретной смены: какие сцены снимаем сегодня.

### 📂 Логика и Парсеры (Logic)
*   `composeApp/src/commonMain/kotlin/org/mosyagin/project/parser/`
    *   `ScriptParser.kt` — Логика извлечения сцен, персонажей и времени суток из текста сценария.
    *   `KppParser.kt` — Логика чтения CSV-файлов КПП и привязки смен к сценам в базе.
*   `composeApp/src/commonMain/kotlin/org/mosyagin/project/ui/screens/ScriptViewModel.kt` — Управляет процессом загрузки и обработки PDF-файлов.

### 📂 Технические детали
*   `composeApp/src/commonMain/kotlin/org/mosyagin/project/db/DatabaseAdapter.kt` — Настройка подключения к базе данных.
*   `composeApp/src/commonMain/kotlin/org/mosyagin/project/util/FilePicker.kt` — Утилита для выбора файлов в памяти телефона.

---
## 🚀 Как это работает (Workflow)
1. **Создаем проект** в списке проектов.
2. **Загружаем Сценарий (PDF)** — приложение "разрезает" его на сцены и находит актеров.
3. **Загружаем КПП (CSV)** — приложение понимает, в какую смену какую сцену мы снимаем.
4. **Используем Трекер** — на площадке видим план на день и текст нужных сцен в один клик.
