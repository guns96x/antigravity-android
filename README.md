# Antigravity Android Native Companion

Нативний Android-додаток (Kotlin + Jetpack Compose + Material 3) для веб-сервісу Google Antigravity IDE (`antigravity.google`).

Створений як якісна альтернатива PWA з усуненням головних недоліків браузерного клієнта:
- **Без повторного вибору акаунта (No AccountChooser loop)**: надійне збереження сесії та cookies у приватному сховищі додатка. Автоматичний пропуск та вибір потрібного Google-акаунта без щоразового тапу.
- **Підміна User-Agent**: обхід помилки `403 disallowed_useragent` через використання актуального Chrome Mobile стрінгу.
- **Edge-to-Edge та Window Insets**: використання максимальної площі екрана смартфона.
- **Швидкі утиліти**:
  - 🔄 Швидке перезавантаження сесії
  - 🏠 Миттєве повернення до канонічного дашборду
  - 💻/📱 Перемикання між десктопним та мобільним відображенням
  - ☀️ Keep Screen Awake (запобігає засинанню екрана під час довгих тасок агента)
  - 📎 Повна підтримка завантаження файлів (`WebChromeClient`)
  - ⚙️ Зручне керування сесією та очищення cookies при потребі

## Збірка та встановлення

### Збірка APK через Gradle:
```bash
./gradlew.bat assembleRelease
```
Готовий APK формується у:
`app/build/outputs/apk/release/app-release.apk`

### Встановлення на пристрій через ADB:
```bash
adb install app/build/outputs/apk/release/app-release.apk
```
Або просто передайте APK-файл на телефон та встановіть.
