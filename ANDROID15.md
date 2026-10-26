# Alite 1.5.9 – Android-15-Port

Entwicklungsbranch: `android15-port`; `master` wird nicht verändert. Original: [CmdrStardust/Alite](https://github.com/CmdrStardust/Alite). GPL-3.0-or-later, siehe `license.txt` und vorhandene Copyright-/Asset-Hinweise.

## Bestandsaufnahme und Hindernisse

- Eclipse/Ant, Android-19-Buildziel, targetSdk 22/minSdk 12, eingecheckte generierte R/BuildConfig-Dateien. Gradle benutzt ausschließlich `src`, `res` und `assets`; `gen` wird nicht kompiliert.
- 373 Quelldateien: originale Java-Spielarchitektur, OpenGL ES 1.1/GLES11, eigener 1920×1080-Zeichenraum, Multitouch/Gamepad, zwei Beschleunigungssensor-Steuerungen, SoundPool/MediaPlayer. Keine JNI-Bibliotheken, JARs, NDK- oder 32-Bit-Abhängigkeiten.
- Veraltete Play-Lizenz-/Expansion-/Downloader-Quellen enthalten unter anderem entfernte HTTP-/Benachrichtigungs-APIs, alte PendingIntents, BroadcastReceiver und Hintergrunddienste. Die bereits ursprünglich aktivierte Offline-Verteilung braucht sie nicht: Downloader und Lizenzbibliothek werden nicht mitgebaut, nicht registriert und nicht ausgeführt. Der Launcher startet lokal. Optionale alte OBB-/OXP-Hilfen bleiben als historische Quellen erhalten; OBB-Downloads werden im Port nicht unterstützt.
- Alte Speicherimplementierung konnte bei fehlendem externem Speicher Unterverzeichnisse verlieren, Nullverzeichnisse dereferenzieren und Teilinhalte ungeprüft lesen. Der Renderer ignorierte Surface-Größenänderungen; Displaymaße und Touchkoordinaten konnten von der tatsächlichen Zeichenfläche abweichen. Ein Frame-Limit verwendete aktives Warten. Audio hatte asynchrone Vorbereitungs-/Pause-Rennen und freigegebene Player konnten erneut verwendet werden.
- Alle Laufzeitressourcen sind enthalten: **1.380 Dateien / ca. 239,5 MiB unter `assets`**, sechs Intro-Videos / ca. 144,2 MiB unter `res/raw`. `Resources/assets` ist eine zweite historische Kopie und wird nicht zusätzlich verpackt. Keine zusätzlichen OBBs, Play-Dateien oder anderen Spieldownloads erforderlich. Die Rechtehinweise der eingebundenen Drittinhalte bleiben unverändert; dies ist keine unabhängige Rechteprüfung.

## Änderungen

- Reproduzierbarer Gradle Wrapper **9.6.0** mit SHA-256, **AGP 9.4.1**, **JDK 17**, Build Tools **36.0.0**, **compileSdk/targetSdk 35**, **minSdk 23**. AndroidX Test 1.7.0 / JUnit Extension 1.3.0 nur für Instrumentierung. Keine neue Laufzeitbibliothek, Engine, Kotlin- oder Compose-Migration. [AGP-Kompatibilität](https://developer.android.com/build/releases/agp-9-4-0-release-notes).
- Explizites `exported`, nur Launcher öffentlich, OpenGL-Anforderung, Landscape, nur Vibrationsberechtigung. Keine Netzwerk-/Speicher-/Play-/Wakelock-Rechte und keine Dienste/Receiver. Keine Werbung, Tracker oder Online-Dienste.
- App-interner Speicher mit Unterverzeichnissen für Einstellungen, Commander und Zustand. Einmalige Kopiermigration aus dem bisherigen eigenen externen App-Verzeichnis; bestehende interne Dateien werden nicht überschrieben, Originaldateien nicht gelöscht. Alte flach gespeicherte interne `.cmdr`-Dateien werden ebenfalls kopiert. Marker verhindert das Wiederherstellen bewusst gelöschter alter Commander. Gemeinsame fremde Speicherbereiche werden nicht durchsucht.
- Touch und GL-Viewport verwenden die tatsächliche Surface-Größe. Android-15-Cutout-/Navigationsleisten-Inset-Behandlung, moderne Immersion-API, korrigierte Zentrierung mit nichtnull Ursprung und lokale Swipe-Koordinaten. Oberfläche und Texturen werden bei Größen-/Kontextwechsel über den bestehenden Spielzustand wiederhergestellt. Originales 30-FPS-Ziel ohne CPU-Dauerschleife. [Android-15-Fensterregeln](https://developer.android.com/about/versions/15/behavior-changes-15).
- Sensoren mit `SENSOR_DELAY_GAME`, vorhandene Kalibrierung und Abmeldung bleiben erhalten. SoundPool Builder, AudioAttributes, Audiofokus nur im Vordergrund, Pause/Weiterlauf/Freigabe und sichere asynchrone Musikvorbereitung; Asset-Deskriptoren werden geschlossen. Intro startet den Player auf dem UI-Thread.
- Sämtliche Originalassets und Videoqualitäten bleiben unverändert. Audio/Video werden für direkten AssetFileDescriptor-Zugriff unkomprimiert gespeichert; andere Dateien erhalten die normale verlustfreie ZIP-Komprimierung. Dateigröße rechtfertigt keine Qualitätsreduktion. Single-APK, offline ab erstem Start.
- `.github/workflows/android-build.yml`: Build, Unit-Tests, Lint, APK-Signatur-/Inhaltsprüfung und Android-15-Emulatorjob. Separate APK-/Berichts-/Emulator-Artefakte; Emulatorfehler verhindern den APK-Download nicht. Debug-Schlüssel entstehen ausschließlich temporär, werden nicht eingecheckt oder als Artefakt hochgeladen.

## Bauen

JDK 17 und Android SDK installieren; `ANDROID_HOME` setzen:

```sh
sdkmanager 'platforms;android-35' 'build-tools;36.0.0' 'platform-tools'
./gradlew --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
bash scripts/verify-apk.sh build/outputs/apk/debug/Alite-debug.apk
```

Lokale APK: `build/outputs/apk/debug/Alite-debug.apk`. GitHub Actions: Workflow **Alite Android 15**, Branch **android15-port**, Artefakt **Alite-1.5.9-Android15-debug.apk** herunterladen und ZIP entpacken. Darin liegen die gleichnamige APK und eine SHA-256-Datei. Artefaktaufbewahrung: 90 Tage. Ein manueller Workflow-Start ist nach Registrierung des Workflows im Default-Branch möglich; Push/PR lösen ihn bereits im Entwicklungsbranch aus.

## Installation auf dem POCO F6

1. Vorhandene Spielstände einer alten Installation sichern. Diese Debug-APK kann die ursprüngliche Play-App wegen anderer Signatur nicht aktualisieren. Die gleiche Paket-ID erhält die Spielarchitektur und Dateikompatibilität; eine Deinstallation löscht App-Daten. Keinesfalls ohne Sicherung deinstallieren.
2. APK aus dem entpackten Artefakt auf das Gerät übertragen, öffnen und für die benutzte Datei-/Browser-App „Unbekannte Apps installieren“ erlauben. Alternativ: `adb install -r Alite-1.5.9-Android15-debug.apk`.
3. Starten; kein zusätzlicher Download und keine Speicherfreigabe nötig. Erst Intro, dann „Tap to start“. Steuerung/Screen-Lock in den Originaloptionen einstellen.
4. Neue Daten liegen unter dem app-internen `files/`-Verzeichnis; die alte eigene externe Datenablage wird bei erhaltenen App-Daten einmalig übernommen. Für die Debug-Version ist eine Sicherung mit `adb exec-out run-as de.phbouillon.android.games.alite tar -C files -cf - . > alite-backup.tar` möglich. Nicht auf veraltetes `adb backup` verlassen.

Der Standard-Debug-Schlüssel ist kein dauerhafter Veröffentlichungsschlüssel. Frische CI-Runner können unterschiedliche Debug-Schlüssel erzeugen; ein Update zwischen solchen Builds kann deshalb eine vorherige Sicherung/Neuinstallation erfordern. Dauerhafte Releases benötigen einen separat verwalteten Signaturschlüssel aus GitHub Secrets; ein Release-Workflow ist noch nicht eingerichtet.

## Teststatus

Lokal bisher erfolgreich: Debug-APK gebaut, zwei JVM-Galaxietests bestanden, APK-Inhalt/Signatur/SDK/ZIP-Ausrichtung geprüft. CI und Emulator werden noch geprüft. Lint meldete zunächst ausschließlich Fehler im nicht kompilierten Play-Archiv; die Archivpfade sind deshalb gezielt von Lint ausgenommen, ohne Laufzeitprüfungen abzuschalten. Testumfang:

- JVM: ursprüngliche Galaxie/Lave und deterministische Erzeugung aller acht Galaxien.
- Paketprüfung: jedes Originalasset bytegleich, alle sechs Videos, unkomprimiertes Audio, keine nativen ABIs, Manifest SDK 23/35, Signatur und ZIP-Ausrichtung.
- Android 15: isolierte Speichermigration, Unterverzeichnisse, Null-externes-Verzeichnis, Dateibereiche/EOF und ZIP-Pfadschutz. Launcher-Test: gebündeltes Intro tatsächlich abspielen und zum Spiel wechseln. Spieltest: Activity-Start, GL-Fläche mit farbiger Ausgabe/Screenshots, echter Touch auf „Tap to start“, Original-Commander speichern/laden, Handels-/Galaxie-/Flugansicht und Pause/Wiederaufnahme mit gespeichertem Flugzustand.
- **Physisches POCO F6/HyperOS: nicht getestet.** Audioqualität, reale Neigungssteuerung, Cutout/Gestenleisten in beiden Landscape-Richtungen, lange Flüge/Kämpfe, alle Missionen und Frametiming bleiben Gerätetests. Kompilierung, Installation, Start und ein automatisierter Spiel-Smoke-Test sind verschiedene Nachweisstufen; vollständige Spielbarkeit wird dadurch nicht garantiert.
