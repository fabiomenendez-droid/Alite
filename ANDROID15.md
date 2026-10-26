# Alite 1.5.9 – Android-15-Port

Entwicklungsbranch: `android15-port`; `master` wird nicht verändert. Original: [CmdrStardust/Alite](https://github.com/CmdrStardust/Alite). GPL-3.0-or-later, siehe `license.txt` und vorhandene Copyright-/Asset-Hinweise.

**Testbare APK:** [Alite-1.5.9-Android15-debug.apk herunterladen](https://github.com/fabiomenendez-droid/Alite/actions/runs/40287808555/artifacts/12634698429). GitHub-Artefakt als ZIP herunterladen und entpacken. Die signierte APK ist ca. 385,2 MiB groß und enthält sämtliche Spieldaten. [Build und zusätzliche Emulatorprüfung](https://github.com/fabiomenendez-droid/Alite/actions/runs/40287808555); [Entwurfs-PR nach master](https://github.com/fabiomenendez-droid/Alite/pull/1), nicht gemergt. Die APK wird im erfolgreichen Build-Job bereitgestellt und hängt nicht vom Ergebnis des anschließenden Emulatorjobs ab.

## Bestandsaufnahme und Hindernisse

- Eclipse/Ant, Android-19-Buildziel, targetSdk 22/minSdk 12, eingecheckte generierte R/BuildConfig-Dateien. Gradle benutzt ausschließlich `src`, `res` und `assets`; `gen` wird nicht kompiliert.
- 373 Quelldateien: originale Java-Spielarchitektur, OpenGL ES 1.1/GLES11, eigener 1920×1080-Zeichenraum, Multitouch/Gamepad, zwei Beschleunigungssensor-Steuerungen, SoundPool/MediaPlayer. Keine JNI-Bibliotheken, JARs, NDK- oder 32-Bit-Abhängigkeiten.
- Veraltete Play-Lizenz-/Expansion-/Downloader-Quellen enthalten unter anderem entfernte HTTP-/Benachrichtigungs-APIs, alte PendingIntents, BroadcastReceiver und Hintergrunddienste. Die bereits ursprünglich aktivierte Offline-Verteilung braucht sie nicht: Downloader und Lizenzbibliothek werden nicht mitgebaut, nicht registriert und nicht ausgeführt. Der Launcher startet lokal. Optionale alte OBB-/OXP-Hilfen bleiben als historische Quellen erhalten; OBB-Downloads werden im Port nicht unterstützt.
- Alte Speicherimplementierung konnte bei fehlendem externem Speicher Unterverzeichnisse verlieren, Nullverzeichnisse dereferenzieren und Teilinhalte ungeprüft lesen. Der Renderer ignorierte Surface-Größenänderungen; Displaymaße und Touchkoordinaten konnten von der tatsächlichen Zeichenfläche abweichen. Ein Frame-Limit verwendete aktives Warten. Audio hatte asynchrone Vorbereitungs-/Pause-Rennen und freigegebene Player konnten erneut verwendet werden.
- Alle vorgesehenen Laufzeitressourcen sind enthalten: **1.380 Dateien / ca. 239,5 MiB unter `assets`**, sechs Intro-Videos / ca. 144,2 MiB unter `res/raw`. `Resources/assets` ist per SHA-256 als identische zweite Kopie bestätigt und wird nicht zusätzlich verpackt. Keine zusätzlichen OBBs, Play-Dateien oder anderen Spieldownloads erforderlich. Die statische Suche fand außerdem eine fehlende `textures/hognose.png` im unbenutzten alten Hognose-Modell; die 1.5.9-Fabrik und Schiffsvorschau verwenden durchgehend Hognose2 mit vorhandener Textur. Dieses Altmodell wurde nicht durch eine erfundene Ressource ersetzt. Die Rechtehinweise der eingebundenen Drittinhalte bleiben unverändert; dies ist keine unabhängige Rechteprüfung.

## Änderungen

- Reproduzierbarer Gradle Wrapper **9.6.0** mit SHA-256, **AGP 9.4.1**, **JDK 17**, Build Tools **36.0.0**, **compileSdk/targetSdk 35**, **minSdk 23**. AndroidX Test 1.7.0 / JUnit Extension 1.3.0 nur für Instrumentierung. Keine neue Laufzeitbibliothek, Engine, Kotlin- oder Compose-Migration. [AGP-Kompatibilität](https://developer.android.com/build/releases/agp-9-4-0-release-notes).
- Explizites `exported`, nur Launcher öffentlich, OpenGL-Anforderung, Landscape, nur Vibrationsberechtigung. Keine Netzwerk-/Speicher-/Play-/Wakelock-Rechte und keine Dienste/Receiver. Keine Werbung, Tracker oder Online-Dienste.
- App-interner Speicher mit Unterverzeichnissen für Einstellungen, Commander und Zustand. Einmalige Kopiermigration aus dem bisherigen eigenen externen App-Verzeichnis; bestehende interne Dateien werden nicht überschrieben, Originaldateien nicht gelöscht. Alte flach gespeicherte interne `.cmdr`-Dateien werden ebenfalls kopiert. Marker verhindert das Wiederherstellen bewusst gelöschter alter Commander. Gemeinsame fremde Speicherbereiche werden nicht durchsucht.
- Touch und GL-Viewport verwenden die tatsächliche Surface-Größe. Android-15-Cutout-/Navigationsleisten-Inset-Behandlung, moderne Immersion-API, korrigierte Zentrierung mit nichtnull Ursprung und lokale Swipe-Koordinaten. Oberfläche und Texturen werden bei Größen-/Kontextwechsel über den bestehenden Spielzustand wiederhergestellt. Originales 30-FPS-Ziel ohne CPU-Dauerschleife. [Android-15-Fensterregeln](https://developer.android.com/about/versions/15/behavior-changes-15).
- Sensoren mit `SENSOR_DELAY_GAME`, vorhandene Kalibrierung und Abmeldung bleiben erhalten. SoundPool Builder, AudioAttributes, Audiofokus nur im Vordergrund, Pause/Weiterlauf/Freigabe und sichere asynchrone Musikvorbereitung; Asset-Deskriptoren werden geschlossen. Intro startet den Player auf dem UI-Thread.
- Sämtliche Originalassets und Videoqualitäten bleiben unverändert. Audio/Video werden für direkten AssetFileDescriptor-Zugriff unkomprimiert gespeichert; andere Dateien erhalten die normale verlustfreie ZIP-Komprimierung. Dateigröße rechtfertigt keine Qualitätsreduktion. Single-APK, offline ab erstem Start.
- `.github/workflows/android-build.yml`: Build, Unit-Tests, Lint, APK-Signatur-/Inhaltsprüfung und Android-15-Emulatorjob. Separate APK-/Berichts-/Emulator-Artefakte; Emulatorfehler verhindern den APK-Download nicht. Die Emulatorprüfung verwendet Android 15/API 35, x86_64, und den festgelegten stabilen Emulator 35.4.9 (Google-Build 13025442). Laufendes Logcat und begrenzte ADB-/Testlaufzeiten sichern Diagnosen auch bei Verbindungsverlust. Debug-Schlüssel entstehen ausschließlich temporär, werden nicht eingecheckt oder als Artefakt hochgeladen.

## Bauen

JDK 17 und Android SDK installieren; `ANDROID_HOME` setzen:

```sh
sdkmanager 'platforms;android-35' 'build-tools;36.0.0' 'platform-tools'
./gradlew --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
bash scripts/verify-apk.sh build/outputs/apk/debug/Alite-debug.apk
```

Lokale APK: `build/outputs/apk/debug/Alite-debug.apk`. GitHub Actions: Workflow **Alite Android 15**, Branch **android15-port**, Artefakt **Alite-1.5.9-Android15-debug.apk** herunterladen und ZIP entpacken. Darin liegen die gleichnamige APK und eine SHA-256-Datei. Artefaktaufbewahrung: 90 Tage. Ein manueller Workflow-Start ist nach Registrierung des Workflows im Default-Branch möglich; Code-Pushes lösen ihn bereits im Entwicklungsbranch aus; reine Markdown-Änderungen starten keinen neuen Build.

## Installation auf dem POCO F6

1. Vorhandene Spielstände einer alten Installation sichern (die bisherige externe Ablage ist `Android/data/de.phbouillon.android.games.alite/files`; wenn über USB/ADB zugänglich, vor der Deinstallation auf den Rechner kopieren). Diese Debug-APK kann die ursprüngliche Play-App wegen anderer Signatur nicht aktualisieren. Die gleiche Paket-ID erhält die Spielarchitektur und Dateikompatibilität; eine Deinstallation löscht App-Daten. Keinesfalls ohne Sicherung deinstallieren.
2. APK aus dem entpackten Artefakt auf das Gerät übertragen, öffnen und für die benutzte Datei-/Browser-App „Unbekannte Apps installieren“ erlauben. Alternativ: `adb install -r Alite-1.5.9-Android15-debug.apk`.
3. Starten; kein zusätzlicher Download und keine Speicherfreigabe nötig. Erst Intro, dann „Tap to start“. Steuerung/Screen-Lock in den Originaloptionen einstellen.
4. Neue Daten liegen unter dem app-internen `files/`-Verzeichnis; die alte eigene externe Datenablage wird bei erhaltenen App-Daten einmalig übernommen. Für die Debug-Version ist eine Sicherung mit `adb exec-out run-as de.phbouillon.android.games.alite tar -C files -cf - . > alite-backup.tar` möglich. Nicht auf veraltetes `adb backup` verlassen.

Der Standard-Debug-Schlüssel ist kein dauerhafter Veröffentlichungsschlüssel. Frische CI-Runner können unterschiedliche Debug-Schlüssel erzeugen; ein Update zwischen solchen Builds kann deshalb eine vorherige Sicherung/Neuinstallation erfordern. Dauerhafte Releases benötigen einen separat verwalteten Signaturschlüssel aus GitHub Secrets; ein Release-Workflow ist noch nicht eingerichtet.

## Teststatus

Stand der Bereitstellung am 26.10.2026:

| Prüfstufe | Tatsächliches Ergebnis |
| --- | --- |
| Kompilierung | Lokal und in GitHub Actions erfolgreich; App- und Instrumentierungs-APK gebaut. |
| JVM-Tests | 2 bestanden: ursprüngliche Galaxie/Lave und deterministische Erzeugung aller acht Galaxien. |
| APK-Prüfung | Erfolgreich: ZIP-Integrität, jedes Originalasset bytegleich, alle sechs Videos, unkomprimiertes Audio, keine nativen Bibliotheken, Manifest SDK 23/35, Debug-Signatur und ZIP-Ausrichtung. |
| Android-15-Installation | App und Test-APK auf Android 15 x86_64 erfolgreich installiert. |
| Android-Speichertests | Alle 4 bestanden, erneut mit vollständigem `OK (4 tests)` im oben verlinkten aktuellen Lauf. Migration, Unterverzeichnisse, fehlender externer Speicher, Dateibereiche/EOF und ZIP-Pfadschutz geprüft. |
| Launcher/Intro | Ein früherer Lauf bestätigte die Videowiedergabe und den Wechsel zur Spiel-Activity als einzelnen bestandenen Test; wegen anschließendem Emulator-Verbindungsverlust kein vollständig bestandener Gesamtlauf. |
| Musik | Separater Android-15-Test bestanden (`OK (1 test)`): Original-MP3 vorbereiten/abspielen, Wiedergabe sperren/freigeben, pausieren, erneut starten, stoppen und mehrfach sicher freigeben. Kein Nachweis der hörbaren Qualität auf dem POCO. |
| Spielstart/Grafik/Touch | Mit Mesa erfolgreich bis in den Flug: Schiffsvorschau, echter Touch auf „Tap to start“, Status-, Handels-, Galaxie- und Flugansicht. Farbige Screenshots sind im Emulator-Artefakt vorhanden; Flugansicht zusätzlich visuell geprüft. |
| Commander speichern/laden | Original-Commander im Integrationstest gespeichert und zurückgeladen; Name und Guthaben korrekt wiederhergestellt. |
| Flug-Pause/Wiederaufnahme | Test fehlgeschlagen: nach Pause und Wiederaufnahme monochromes/schwarzes Bild im Screenshot (`Blank/monochrome output: 1`). Zustand wurde geschrieben und FlightScreen deserialisiert; Ursache des schwarzen Bilds und Dauer/Übertragbarkeit auf das POCO ungeklärt. Kein vollständig bestandener Spiel-Integrationstest. |
| Physisches POCO F6/HyperOS | Nicht getestet. |

Der bekannte Speicher-Testfehler war ein Vergleich der Android-Pfadaliase `/data/data` und `/data/user/0`. Der Test vergleicht jetzt beidseitig kanonische Pfade; die Pfadschutzprüfung bleibt erhalten. Die Korrektur ist in `android15-port` eingecheckt und durch die vier bestandenen Gerätetests bestätigt.

Lint: 0 Fehler, 127 überwiegend ältere Formatierungs-/Locale-/Ressourcenwarnungen. Lint meldete zunächst Fehler im nicht kompilierten Play-Archiv; nur diese Archivpfade sind gezielt ausgenommen, ohne Laufzeitprüfungen abzuschalten.

## Offene funktionale Prüfung

Die APK ist zur Installation und zum Test auf dem POCO F6 bereit, **vollständige Spielbarkeit ist noch nicht bestätigt**. Konkreter offener Befund ist das schwarze Bild im Wiederaufnahme-Test. Die früheren SwiftShader-Läufe verloren beim Ressourcenladen den gesamten Emulator; Mesa erreicht dagegen die Flugansicht. Daraus lässt sich kein sicherer Befund für den POCO-Treiber ableiten. [Diagnoseartefakt mit Screenshots und Testprotokollen](https://github.com/fabiomenendez-droid/Alite/actions/runs/40287808555/artifacts/12634704134).

Für den Gerätetest zuerst Intro/„Tap to start“, Handels- und Galaxieansicht, Start ins All sowie Commander speichern/laden und insbesondere App-Pause/Wiederaufnahme prüfen. Danach Audio/Unterbrechungen, reale Neigungssteuerung, Cutout/Gestenleisten in beiden Landscape-Richtungen, lange Flüge/Kämpfe, Missionen und Frametiming prüfen. Keine weiteren CI-Umbauten sind für die Bereitstellung erforderlich. Der Build-Job ist erfolgreich, der zusätzliche Emulatorjob wegen der Wiederaufnahme-Prüfung fehlgeschlagen; die APK bleibt herunterladbar.
