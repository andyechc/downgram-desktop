# Downgram Desktop

Desktop app to search and download media from Telegram channels, groups, and chats. Built with Kotlin/Compose Multiplatform (Cupertino design) + Python/FastAPI backend.

![macOS](https://img.shields.io/badge/platform-macOS-purple) ![Windows](https://img.shields.io/badge/platform-Windows-purple)

## Features

- Cupertino-style UI (purple theme, light/dark/auto)
- Search media across Telegram dialogs
- Download with progress and concurrent limit control
- Import session via `.dwngm` file
- Export as DMG (macOS) or MSI (Windows)

## Requirements

- **Java 17** (Zulu or Temurin)
- **Python 3.12+** (only for development or when running without PyInstaller binary)

## Quick Start (Development)

```bash
export JAVA_HOME=/path/to/zulu-17.jdk/Contents/Home  # macOS
# or
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-17"      # Windows

./gradlew :composeApp:run
```

The app will auto-launch the Python backend (via `venv` or system Python).

## Package for Distribution

### macOS — DMG

```bash
export JAVA_HOME=/path/to/zulu-17.jdk/Contents/Home
cd composeApp/backend
python -m venv venv
venv/bin/pip install -r requirements.txt pyinstaller
venv/bin/pyinstaller downgram-backend.spec
cp dist/downgram-backend ../backend-bin/common/
cd ../..
./gradlew :composeApp:packageDmg
```

DMG → `composeApp/build/compose/binaries/main/dmg/Downgram-*.dmg`

### Windows — MSI

```powershell
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-17"
cd composeApp/backend
python -m venv venv
venv\Scripts\pip install -r requirements.txt pyinstaller
venv\Scripts\pyinstaller downgram-backend.spec
copy dist\downgram-backend.exe ..\backend-bin\common\
cd ..\..
gradlew :composeApp:packageMsi
```

MSI → `composeApp\build\compose\binaries\main\msi\Downgram-*.msi`

### CI (GitHub Actions)

On every push to `main`, a workflow builds and releases both platforms automatically.

## Project Structure

```
composeApp/
├── backend/                          # Python/FastAPI backend
│   ├── main.py                       # FastAPI server + WebSocket
│   ├── telegram_client.py            # Telethon client
│   ├── downloader.py                 # Download manager
│   ├── config.py                     # Configuration
│   └── downgram-backend.spec         # PyInstaller spec
├── backend-bin/common/               # Packaged backend binaries
│   ├── downgram-backend              # PyInstaller binary (macOS)
│   └── *.py                          # Python source fallback
└── src/jvmMain/kotlin/com/andyechc/downgram/
    ├── main.kt                       # App entry + navigation
    ├── BackendService.kt             # Spawns Python backend
    ├── data/
    │   ├── model/Models.kt           # Data models
    │   ├── repository/
    │   │   ├── TelegramRepository.kt # HTTP/WS client
    │   │   └── SettingsRepository.kt # Java Prefs persistence
    ├── ui/
    │   ├── components/               # Cupertino components
    │   ├── screen/                   # App screens
    │   ├── theme/                    # Purple color scheme
    │   ├── viewmodel/                # MVVM view models
    │   └── utils/FormatUtils.kt     # Size formatting
```

## Notes

- **macOS Gatekeeper**: the unsigned DMG requires right-click → Open on first launch.
- **Apple Silicon only**: the included PyInstaller binary is arm64. Rebuild for Intel if needed.
- **Credentials**: not embedded. Each user imports a `.dwngm` file or enters API ID/Hash manually.
