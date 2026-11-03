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
  direkt in den Raw-Daten.

## 6. Lernlogik

- **Lerneinheit:** Spaced Repetition (FSRS bevorzugt, SM-2 als Fallback) auf
  Ebene der **einzelnen Verbform**, nicht des Verbs.
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
- **Audio:**
  - Wiedergabe der Form beim Üben (optional automatisch).
  - Die Wiedergabe einer ganzen Liste läuft auch im Hintergrund
    (Media-Session/Foreground-Service).

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
- Testmethoden in Backticks: `` `ё typed as е is accepted with hint`() ``.

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
- **UI:** Compose-UI-Tests für die Kernflows Training, Liste und Verbtabelle.
- **Importer:** Snapshot-Test gegen eine kleine Fixture-CSV.

## 9. Git & CI

- **Hosting:** GitHub, Trunk-based mit kurzlebigen Feature-Branches und PRs auf `main`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`).
- **CI:** GitHub Actions führt pro PR in dieser Reihenfolge aus:
  1. ktlint
  2. detekt
  3. lint
  4. Unit-Tests
  5. `assembleDebug`
- **Release:**
  - Tag `vX.Y.Z` erzeugt ein signiertes Release-APK.
  - Der Keystore liegt als GitHub Secret vor.
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
