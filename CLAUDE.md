# CLAUDE.md – Glagolik (глаголик)

- **Repo:** `eleasar-b/glagolik`
- **Package / applicationId:** `io.github.eleasarb.glagolik`
- **Lizenz:** Apache-2.0 für den Code, CC BY-SA 4.0 für `data/` (siehe §2)

Leitplanken für die Entwicklung. Gilt für Menschen und für Claude.
Bei Widerspruch zwischen diesem Dokument und einer Anweisung im Chat: nachfragen.

## 1. Ziel

Android-App zum Trainieren russischer Verbformen (Konjugation, Aspekt, Imperativ).

- **v1-Umfang:** die 600 häufigsten Verben, mit Präsens/Futur, Präteritum,
  Imperativ, Aspekt und Aspektpartner.
- **Verteilung:** zuerst privat (signiertes APK). Ein späteres Release im
  Play Store wird von Anfang an mitgedacht.

## 2. Guardrails (nicht verhandelbar)

1. **Keine fremden geschützten Inhalte.** Daten, Texte, Audio und Grafiken aus
   der Russlandjournal-App oder anderen proprietären Quellen werden nicht übernommen.
2. **Verbformen kommen nie aus einem LLM.** Alle Formen stammen aus dem Datensatz.
   Regelbasiert erzeugt werden darf nur das zusammengesetzte Futur
   imperfektiver Verben (буду/будешь/… + Infinitiv).
3. **Datenquelle:** OpenRussian-Dump (CC BY-SA 4.0).
   - Attribution steht in `ATTRIBUTION.md` und im Info-Screen der App.
   - Der abgeleitete Datensatz (`data/`) bleibt CC BY-SA 4.0.
   - Die Audio-Links von OpenRussian sind **nicht** frei und werden nicht verwendet.
4. **Betonung ist Pflicht.** Jede Form trägt ihre Betonung (U+0301 COMBINING ACUTE).
   Fehlt die Betonung, schlägt der Import fehl.
5. **Privacy by design:**
   - keine `INTERNET`-Permission
   - kein Tracking, keine Analytics, keine Crash-Reporter
   - keine Google-Play-Services-Abhängigkeit (muss auf GrapheneOS laufen)
6. **Keine neuen Abhängigkeiten ohne Rückfrage.** Jede neue Library wird im PR begründet.
7. **Keine Secrets im Repo.** Keystore und Passwörter liegen nur lokal bzw. in
   GitHub Secrets.

## 3. Tech-Stack

| Bereich       | Wahl                                                  |
|---------------|-------------------------------------------------------|
| Sprache       | Kotlin (aktuelle stabile Version)                     |
| UI            | Jetpack Compose, Material 3, Navigation Compose       |
| Architektur   | Single-Activity, MVVM + UDF, AAC `ViewModel`          |
| Persistenz    | Room (vorbefüllte DB aus Asset), DataStore (Settings) |
| Async         | Coroutines + Flow                                     |
| DI            | Hilt (Abhängigkeit explizit freigegeben)              |
| Audio         | Android `TextToSpeech` (ru-RU, offline)               |
| Build         | Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`) |
| SDK           | `compileSdk`/`targetSdk` 37, `minSdk` 26              |

`targetSdk` 37 ist das neueste stabile API-Level und liegt über der Play-Anforderung
(mindestens 36 ab 31.08.2026 für neue Apps und Updates).

## 4. Architektur

Gemäss den offiziellen Android Architecture Recommendations:

- **Layer:** `ui` → `domain` → `data`. Der Domain-Layer ist reines Kotlin,
  ohne Android-Imports.
- **Repositories:** Composables und ViewModels greifen nie direkt auf Room oder
  DataStore zu, sondern immer über Repositories.
- **UI-State:**
  - Jedes ViewModel exponiert genau ein `uiState: StateFlow<XyzUiState>`.
  - Aktionen kommen als Methodenaufrufe ins ViewModel (UDF).
- **State sammeln:** in der UI nur mit `collectAsStateWithLifecycle()`.
- **ViewModel-Basisklasse:** `ViewModel`, nie `AndroidViewModel`.
- **Modelle:** eigene Modelle pro Layer. Room-Entities verlassen den Data-Layer nicht.
- **Modulstruktur:** vorerst ein App-Modul mit sauberer Package-Trennung.
  Die Aufteilung in Module folgt erst, wenn nötig.

Package-Struktur (Root: `io.github.eleasarb.glagolik`):

```
io/github/eleasarb/glagolik/
  ui/        screens, components, theme, navigation
  domain/    model, srs, checking (Antwortprüfung), usecase
  data/      db, repository, settings, tts
```

## 5. Daten-Pipeline

- **Quellcode:** `tools/importer/`, eine JVM-Kotlin-CLI im selben Gradle-Build.
- **Input:** OpenRussian-CSV-Export, abgelegt unter `data/raw/` und versioniert.
- **Output:** `app/src/main/assets/verbs.db`, eine Room-kompatible SQLite-DB.
- **Auswahl:** die 600 Verben mit dem besten Frequenzrang.
  Die Liste wird als `data/selection.csv` versioniert und ist reviewbar.
- **Validierung** (der Import bricht bei Fehlern ab):
  - Vollständigkeit pro Aspekt: Perfektive Verben haben kein Präsens,
    imperfektive kein einfaches Futur.
  - Jede Form hat genau eine Betonung (Ausnahme: einsilbige Formen und Formen mit ё).
  - Es kommen nur kyrillische Zeichen vor (keine lateinischen Doppelgänger).
  - Aspektpartner verweisen auf existierende Einträge.
- **Korrekturen:** Manuelle Korrekturen stehen in `data/overrides.csv`, nie
  direkt in den Raw-Daten. Aktionen: `EXCLUDE` (Verb weglassen), `ALLOW_MISSING`
  (Formen fehlen legitim, z. B. unpersönliche Verben), `SET` (Feld ersetzen, nur mit
  geprüftem Wert), `NO_TRAINING` (Formen bleiben in der Tabelle, werden aber nie
  abgefragt, z. B. Präsens von быть), `RANK` (Frequenzrang ersetzen, wenn die Quelle die
  Frequenz einem falschen Homographen zuordnet; beide Einträge tauschen, Ränge bleiben
  eindeutig). Jede Zeile braucht einen Grund.
- **Stabile IDs:** `data/verb-ids.csv` ordnet jedem Verb (betonter Infinitiv + Aspekt)
  eine feste ID zu. Lernfortschritt referenziert diese ID. Neue Verben erhalten die
  nächste freie ID, IDs weggefallener Verben bleiben reserviert und werden nie neu
  vergeben. Der Frequenzrang steht separat in `verb.frequency_rank`.
- **Aspektpartner ausserhalb der 600** werden als Referenzverben (`is_core = 0`)
  mitgeliefert, damit die Verlinkung aufgeht. Ungültige Partner werden übersprungen
  und in `data/import-report.md` gelistet.
- **Datenversion:** `data/content-version.txt` hält Version und SHA-256 des Inhalts von
  `verbs.db`. Ändert sich Schema oder Inhalt, erhöht der Importer die Version
  (= `PRAGMA user_version`). `VerbsDatabase.VERSION` muss nachgezogen werden, der
  Schema-Test erzwingt das. Nur so ersetzt Room die installierte Kopie.
- **Ausführen:** `./gradlew :tools:importer:importVerbs` erzeugt `verb-ids.csv`, `selection.csv`,
  `import-report.md` und `verbs.db`. `verifyVerbs` prüft in der CI, dass alles aktuell ist.

## 6. Lernlogik

- **Lerneinheit:** Spaced Repetition mit FSRS-6 auf Ebene der **Karte** = Verb + Übung,
  nicht des Verbs. Eigene Portierung von py-fsrs 6.3.2 (MIT) in `domain/srs`, geprüft
  gegen Referenzwerte aus py-fsrs (`src/test/resources/srs`). Standardparameter,
  Zielretention 0.9, Lernschritte 1 min / 10 min, Wiederlernschritt 10 min.
- **Übungsarten** (`Exercise`, Schlüssel `CardKey` = Verb-ID + Übung):
  - **Form:** eine Verbform tippen (eine Karte pro `FormKey`).
  - **Bedeutung RU→DE** (`MEANING_RU_DE`): Infinitiv gezeigt, Multiple Choice mit 4 Optionen
    (je die ersten 2–3 Bedeutungen). Ablenker: 3 andere Kernverben, nie der Aspektpartner,
    nie ein Verb mit einer gemeinsamen Bedeutung (Deutsch oder Englisch).
  - **Bedeutung DE→RU** (`MEANING_DE_RU`): Übersetzung + Aspekt gezeigt, Infinitiv tippen.
    Ein anderes Kernverb mit gleichem Aspekt und gemeinsamer Bedeutung ist kein Fehler,
    sondern gibt einen Hinweis und einen neuen Versuch.
  - Bedeutungen = Teile der Übersetzung, getrennt durch Komma oder Semikolon (ausserhalb
    von Klammern).
- **Fortschritt:** eigene Datenbank `progress.db` (Tabellen `card`, `review_log`, Version 2),
  getrennt von `verbs.db`. Die Spalte `item` hält die Übungs-ID (`Exercise.id`: der
  `FormKey`-Name oder z. B. `MEANING_RU_DE`); IDs ändern sich nach einem Release nie.
  Nie destruktiv: jede Schema-Änderung braucht eine Migration und einen Migrationstest
  (`ProgressDatabaseMigrationTest`, erzeugt die alte Version aus `app/schemas`).
- **Bewertung:** falsch = Again, richtig = Good, nach richtiger Antwort optional „leicht“ = Easy.
  Gilt für alle Übungsarten.
- **Defaults:** 10 neue Karten pro Tag (ein gemeinsames Limit für alle Übungsarten),
  20 Karten pro Session, „Bedeutung üben“ an (einstellbar, DataStore).
- **Session:** fällige Karten zuerst (am stärksten überfällig zuerst), dann neue Karten: pro
  Verb in Frequenzreihenfolge zuerst RU→DE, dann DE→RU, dann die Formen. Alle Übungsarten
  innerhalb der Session gemischt, RU→DE eines Verbs bleibt dabei vor seinem DE→RU. Die
  Zeitform-Auswahl gilt nur für Formen; ist „Bedeutung üben“ aus, gibt es keine
  Bedeutungskarten (weder fällige noch neue). Der Tag beginnt um Mitternacht Ortszeit.
  Falsch beantwortete Karten kommen am Ende der Session erneut.
- **Backup:** nur `progress.db` und `datastore/settings.preferences_pb` (Cloud-Backup und
  Gerätewechsel, auch Seedvault). `progress.db` läuft im Journal-Modus TRUNCATE, damit
  alles in einer Datei liegt. `verbs.db` wird nie gesichert.
- **Export/Import:** in den Einstellungen, als JSON-Datei (`glagolik-progress`, Version 2,
  siehe `ProgressBackupCodec`) über die Dateiauswahl des Systems (Storage Access
  Framework, keine Berechtigung). Version 2 nennt die Übung im Feld `item`, Version 1 hiess
  es `form` (nur Formen) und wird weiterhin gelesen. Neuere Versionen lehnt der Import mit
  einer Meldung ab. Der Import prüft die ganze Datei zuerst und ersetzt dann Fortschritt und
  Einstellungen in einem Schritt, nach Bestätigung.
- **Sessions:** Die Sessionlänge ist frei wählbar (Anzahl Karten oder Minuten).
  Fällige Karten kommen zuerst, neue Karten sind pro Tag begrenzt.
- **Filter:**
  - Verbliste (vordefiniert oder eigene)
  - Zeitform/Modus
  - Aspekt
  - Status (neu, fällig, schwach)
- **Antwortprüfung** in `domain/checking`, vollständig unit-getestet:
  - Unicode-Normalisierung (NFC), Akzentzeichen werden ignoriert,
    Gross-/Kleinschreibung ebenso.
  - Wird е statt ё eingegeben, gilt die Antwort als korrekt, mit Hinweis
    (das Verhalten ist konfigurierbar).
  - Lateinische Doppelgänger (a/а, e/е, o/о, p/р, c/с, x/х, y/у …) führen zu
    einer klaren Fehlermeldung, die Antwort wird nicht stillschweigend als
    falsch gewertet.
- **Audio:** Wiedergabe der Form beim Üben und in der Verbtabelle (beim Üben optional
  automatisch). Nur russische Offline-Stimmen der System-Sprachausgabe.

## 7. Code-Konventionen

Basis ist der **Android Kotlin Style Guide** (Google). Er baut auf den
**Kotlin Coding Conventions** (JetBrains) auf. Für Composables gelten zusätzlich
die **Compose API Guidelines**.

**Formatierung**
- UTF-8, 4 Leerzeichen Einrückung, keine Tabs, Zeilenlimit 100 Zeichen.
- Klammern im K&R-Stil. Bei `if`/`for`/`when`/`while` sind geschweifte Klammern
  Pflicht. Ausnahme: einzeilige Ausdrücke wie `if (x) a else b`.
- Imports: keine Wildcard-Imports, ASCII-sortiert.
- Trailing Commas bei mehrzeiligen Deklarationen und Argumentlisten.
- Russische Strings stehen als echte Unicode-Zeichen im Code, nicht als
  `\uXXXX`-Escapes. Ausnahme: nicht druckbare Zeichen wie U+0301, die als
  benannte Konstante definiert werden.

**Naming**
- Klassen und Objects in `PascalCase`, Funktionen und Properties in `camelCase`.
- `const val` und Top-Level-Konstanten in `SCREAMING_SNAKE_CASE`.
- Backing Properties mit Unterstrich: `_uiState` / `uiState`.
- Dateinamen:
  - eine Klasse: `Klassenname.kt`
  - mehrere Deklarationen: beschreibender Name in `PascalCase`
- Testmethoden in Backticks: `` `yo typed as ye is accepted with hint`() `` (siehe §8 zu Kyrillisch).

**Compose**
- `@Composable`-Funktionen, die `Unit` zurückgeben, sind `PascalCase`
  (Substantiv), Composables mit Rückgabewert `camelCase`.
- Composables, die intern `remember {}` nutzen und ein Objekt zurückgeben,
  tragen das Präfix `remember…`.
- Parameter-Reihenfolge:
  1. Pflichtparameter
  2. `modifier: Modifier = Modifier` (erster optionaler Parameter)
  3. weitere optionale Parameter
  4. Trailing Lambda
- Screens sind zweigeteilt:
  - `XyzRoute` holt das ViewModel.
  - `XyzScreen` ist stateless und previewbar.
- Jeder stateless Screen hat mindestens eine `@Preview`.

**Idiome**
- `val` statt `var`, read-only Collections nach aussen.
- Expression Bodies für Einzeiler, Named Arguments bei mehreren gleichartigen
  Parametern.
- Kein `!!`. Ausnahme: Tests, dort mit Kommentar begründet.

**Sprache**
- Code, Kommentare, Commits und PRs auf Englisch.
- UI-Strings auf Deutsch und Englisch, ausschliesslich in `strings.xml`.

**Durchsetzung**
- ktlint mit Code-Style `android_studio`, dazu die Compose-Regeln.
- detekt.
- Android Lint mit `warningsAsErrors` für Security- und Correctness-Checks.

## 8. Tests

- **Pflicht:** Unit-Tests für die Domain (`checking`, `srs`), für ViewModels
  inklusive Flows und für die Repositories.
- **Test-Doubles:** Fakes statt Mocks.
- **UI:** Compose-UI-Tests für die Kernflows Training, Liste und Verbtabelle. Sie laufen
  mit Robolectric als normale Unit-Tests auf der JVM (`src/test`), getestet werden die
  stateless `XyzScreen`-Composables. Robolectric läuft auf API 36
  (`robolectric.properties`), bis Espresso API 37 unterstützt.
- **Datenbank:** Ein Robolectric-Test öffnet das gebündelte `verbs.db` über Room, wie
  beim ersten App-Start.
- **Testnamen** in Backticks dürfen kein Kyrillisch enthalten, wenn der Test Lambdas nutzt
  (z. B. `runTest`): Daraus entstehen Klassendateien, die auf Nicht-UTF-8-Dateisystemen
  scheitern.
- **Importer:** Snapshot-Test gegen eine kleine Fixture-CSV.

## 9. Git & CI

- **Hosting:** GitHub, Trunk-based mit kurzlebigen Feature-Branches und PRs auf `main`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`).
- **Merge:** nur Squash-Merge, Commit-Nachricht = PR-Titel. Der PR-Titel muss deshalb ein
  Conventional Commit sein, er landet so im Changelog.
- **CI:** GitHub Actions führt pro PR in dieser Reihenfolge aus:
  1. ktlint
  2. detekt
  3. lint
  4. Unit-Tests (App und Importer), danach `verifyVerbs`
  5. `assembleDebug`
  Das Debug-APK wird als Artefakt hochgeladen (14 Tage). Debug-Builds haben die ID
  `io.github.eleasarb.glagolik.debug`, heissen „Glagolik Debug“ und haben ein
  schiefergraues Icon, damit sie neben der Release-App installiert werden können. Signiert wird es mit dem
  gemeinsamen Debug-Keystore aus dem Secret `GLAGOLIK_DEBUG_KEYSTORE_BASE64`, damit
  Updates ohne Deinstallation funktionieren.
- **Release** (release-please, `.github/workflows/release-please.yml`):
  - Nach jedem Merge auf `main` pflegt release-please einen Release-PR mit nächster
    Version und `CHANGELOG.md`, abgeleitet aus den Conventional Commits
    (`feat` = Minor, `fix` = Patch; vor 1.0 macht ein Breaking Change nur Minor).
  - Merge des Release-PR = Release: Tag `vX.Y.Z` und GitHub Release entstehen, danach
    baut `release.yml` das signierte APK und hängt es samt SHA-256 an.
  - `release.yml` lässt sich für einen bestehenden Tag manuell erneut starten.
  - Version ausschliesslich über release-please, nie Tags von Hand setzen.
  - `versionName` = Tag ohne `v`, `versionCode` = `MAJOR*10000 + MINOR*100 + PATCH`.
  - Signing über die Secrets `GLAGOLIK_KEYSTORE_BASE64`, `GLAGOLIK_KEYSTORE_PASSWORD`,
    `GLAGOLIK_KEY_ALIAS`, `GLAGOLIK_KEY_PASSWORD`. Ohne Secrets bricht der Release ab.
  - R8 ist im Release aktiv.

## 10. Arbeitsweise mit Claude

- **Vorgehen:** kleine, reviewbare Inkremente. Pro Schritt gibt es Code, Tests
  und eine kurze Notiz, was sich geändert hat.
- **Vor dem Umsetzen vorschlagen:**
  - Architektur- und Schemaänderungen (Room-Migrationen!)
  - neue Abhängigkeiten
  - Änderungen an diesem Dokument
- **Bei fehlendem Kontext:** nachfragen statt raten.
- **Bei sprachlichen Zweifeln** (Betonung, Aspektpaar, Form): ein Datenfehler
  wird in `data/overrides.csv` markiert und gemeldet, nicht still korrigiert.

## 11. Offene Punkte

- [x] App-Name: Glagolik
- [x] `minSdk` 26
- [x] Lizenz: Apache-2.0
- [x] Repo-Name: `glagolik`
- [x] Root-Package: `io.github.eleasarb.glagolik` (Bindestrich ist in Packages nicht erlaubt)

## Referenzen

- Kotlin Coding Conventions: https://kotlinlang.org/docs/coding-conventions.html
- Android Kotlin Style Guide: https://developer.android.com/kotlin/style-guide
- Android Architecture Recommendations: https://developer.android.com/topic/architecture/recommendations
- Compose API Guidelines: https://developer.android.com/develop/ui/compose/api-guidelines
- Play Target-API-Anforderungen: https://developer.android.com/google/play/requirements/target-sdk
- OpenRussian Daten: https://en.openrussian.org/dictionary
