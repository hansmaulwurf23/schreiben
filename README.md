# Schreiben

Rechtschreibung für Grundschulkinder auf Android: kleine Schreibspiele mit je fünf Wörtern pro Runde,
Zeitmessung, Punkten und Bestenliste. Läuft auf Handy und auf dem Fire-HD-Kids-Tablet.

Eine bebilderte Kurzbeschreibung steht in [`docs/README.md`](docs/README.md).

## Bedienung

- **Startbildschirm:** eine Kachel je Spiel, darunter ein Beispiel. Ein Tipp startet die Runde.
- **Schreiben:** fünf Wörter nacheinander, ein Fortschrittsbalken zeigt, wie viele erledigt sind.
  Eingabe über die eigene Bildschirmtastatur (QWERTZ mit Ä, Ö, Ü und ß) oder eine
  Hardware-Tastatur, abschicken mit ✓. Am Wortanfang ist die Umschalttaste schon gedrückt, weil
  Nomen großgeschrieben werden; danach gilt sie wie am Handy für einen Buchstaben. Nach jeder
  Antwort kommt kurz *Richtig!* oder die richtige Lösung; ✓ springt dann sofort weiter. Abbrechen
  über ✕ — eine angefangene Runde zählt nicht.
- **Warum eine eigene Tastatur:** Die Systemtastatur würde mit Wortvorschlägen und Autokorrektur
  die Lösung verraten.
- **Verglichen wird genau**, auch Groß- und Kleinschreibung; nur Leerzeichen am Rand zählen nicht.
  Stimmt ein Wort bis auf die Großschreibung („äpfel“), gibt es keinen Punkt, aber den Hinweis
  *Fast! Nomen schreibt man groß*.
- **Zeit:** gemessen wird nur, solange ein Wort auf die Antwort wartet; die Rückmeldung und Pausen
  der App (Bildschirm aus, App gewechselt) zählen nicht.
- **Punkte:** einer je richtig geschriebenes Wort, also höchstens fünf je Runde.
- **Ergebnis:** Punkte, Zeit, alle fünf Wörter mit Lösung (falsche rot, die eigene Antwort
  durchgestrichen daneben) und ggf. *Neuer Rekord!*. Weiter geht es zur Bestenliste.
- **Bestenliste** (Pokal-Symbol): alle Runden, sortiert nach Punkten, bei Gleichstand nach Zeit.
  Filter nach Spiel; die gerade gespielte Runde ist hervorgehoben.
- **Design:** standardmäßig dunkel; *Menü → Dunkles Design* schaltet auf hell um (bleibt gespeichert).

### Spiele

| Spiel | Gezeigt wird | Geschrieben wird |
|---|---|---|
| Mehrzahl | der Apfel | die **Äpfel** |

**Mehrzahl:** Das Kind sieht ein Nomen mit Artikel und schreibt die Mehrzahl. Jede Runde enthält
mindestens zwei Wörter, deren Mehrzahl einen Umlaut bekommt (Apfel – Äpfel, Haus – Häuser), weil
gerade die schwierig sind. Haben Wörter zwei richtige Mehrzahlformen (Wörter/Worte, Bänke/Banken),
zählen beide.

Die Wörter (`game/Nouns`) stammen aus dem **Grundwortschatz für die Jahrgangsstufen 1 und 2** des
bayerischen LehrplanPLUS: alle Nomen daraus mit gebräuchlicher Mehrzahl, gut 90 Stück. Ausgelassen
sind Wörter ohne sinnvolle Mehrzahl (Salz, Gemüse, Eis, Regen …) oder mit unsicherer (Euro, Cent,
Himmel, Nebel). Dazu kommen gut 30 häufige Kinderwörter mit Umlaut in der Mehrzahl (Hand – Hände,
Ball – Bälle, Kuh – Kühe …), die nicht im Grundwortschatz stehen; sie sind in der Liste als `extra`
markiert.

## Bestenliste exportieren und importieren

Für einen Gerätewechsel: *Bestenliste → Menü → exportieren* speichert eine JSON-Datei über den
Android-Dateidialog (z. B. in *Downloads* oder Google Drive). Auf dem neuen Gerät *importieren*
wählen. Der Import **ergänzt**: vorhandene Runden bleiben, schon bekannte (gleiche `id`) werden
übersprungen — mehrfaches Importieren schadet also nicht.

```json
{
  "format": "schreiben-bestenliste",
  "version": 1,
  "games": [
    {"id": "b0fa3f98-…", "timestamp": 1795593000000, "type": "mehrzahl",
     "correct": 4, "total": 5, "points": 4, "millis": 43470}
  ]
}
```

Auf dem Gerät liegt die Liste im selben Format in `files/leaderboard.json` (privater App-Speicher).
Runden mit einem Spiel, das diese Version nicht kennt, bleiben erhalten und werden als „Unbekannt“
angezeigt.

## Neues Spiel hinzufügen

1. Unterklasse von `game/GameType` anlegen: Kennung, Titel, Beispiel für die Kachel, Symbol und
   Farbe im Konstruktor. `createTasks(count, random)` liefert `count` verschiedene `Task`s.
2. In `game/GameTypes` eintragen.

Mehr braucht es nicht — Startbildschirm, Spiel, Ergebnis und Bestenliste richten sich danach. Eine
`Task` besteht aus dem, was gezeigt wird („der Apfel“), einem Präfix vor dem Eingabefeld („die“,
darf leer sein) und einer oder mehreren richtigen Antworten. Damit passen auch Spiele wie
„laufen → er **läuft**“ (Präfix „er“) oder Lückenwörter. Die Kennung landet in Bestenliste und
Export und darf sich nie ändern.

## Technik

- Java, Android Views (kein Compose), Material 3, minSdk 26 / targetSdk 34, damit es auf dem
  Fire-HD-Kids-Tablet (Fire OS 7/8) läuft. Keine Google-Play-Dienste.
- `game/` ist ohne Android-Abhängigkeiten und in `RoundTest`, `TaskTest` und `NounsTest` getestet
  (u. a.: jede Runde hat fünf verschiedene Wörter und mindestens zwei mit Umlaut, jedes Wort der
  Liste kommt irgendwann dran), `stats/Leaderboard` in `LeaderboardTest`.
- Die Tastatur ist eine eigene View (`widget/KeyboardView`), gezeichnet statt aus Buttons gebaut.
- Hochformat fest; auf Tablets (`values-sw600dp`) größere Tasten, Schrift und Kacheln.

## Bauen und installieren

Gebraucht wird ein **JDK 17** (das JBR aus Android Studio ist zu neu):

```bash
JAVA_HOME=~/.jdks/jdk-17.0.20.1+1 ./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Selbst-Update

`update/AppUpdater`: beim Start höchstens alle 6 Stunden und über
*Menü → Nach Update suchen* fragt die App das neueste Release bei der GitHub-API ab
(`/repos/hansmaulwurf23/schreiben/releases/latest`), lädt das APK im Hintergrund, prüft die
SHA-256 aus der angehängten `update.json` und bietet die Installation an. Im
Amazon-Kids-Profil sucht sie gar nicht erst; das Update im Eltern-Profil gilt für alle Profile.
**Die Bestenliste bleibt erhalten**, solange derselbe Signaturschlüssel verwendet wird und
`versionCode` mit jedem Release steigt.

Die API-Adresse steht in `app/build.gradle` (`UPDATE_API`). Ein Umbenennen des Repos leitet GitHub
weiter; wird es gelöscht oder privat, finden ausgelieferte Apps keine Updates mehr.

## Download

Signierte APKs hängen an den [Releases](https://github.com/hansmaulwurf23/schreiben/releases).

Zum Installieren die Datei auf dem Gerät öffnen; auf dem Fire-Tablet im Eltern-Profil installieren
und die App dann für das Kinderprofil freigeben.

## Release

Signaturschlüssel und `keystore.properties` liegen **nicht im Repo**, sondern in
`~/Documents/Android/own/schreiben/` (abweichender Pfad über `-Pschreiben.signing=…`). Fehlt der
Schlüssel, bleibt das Release-APK unsigniert.

Ohne diesen Schlüssel lässt sich kein Update über eine installierte Version schieben — Verlust
bedeutet, dass alle Nutzer erst deinstallieren (und die Bestenliste vorher exportieren) müssten.

`.github/workflows/release.yml` testet, baut, signiert und veröffentlicht, sobald ein Tag `v<Ziffer>`
gepusht wird, und hängt `update.json` neben das APK. Der Text des annotierten Tags (ohne die erste
Zeile) wird zur Release-Beschreibung. Zwei Prüfungen brechen absichtlich ab, statt ein kaputtes
Release zu veröffentlichen: ein unsigniertes APK und ein `versionName`, der nicht zum Tag passt.

Die Repository-Secrets `ANDROID_KEYSTORE_B64` (`base64 -w0 schreiben-release.jks`),
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` (`schreiben`) und `ANDROID_KEY_PASSWORD` sind
gesetzt; damit liegt eine zweite Kopie des Schlüssels bei GitHub.

```bash
# versionCode und versionName in app/build.gradle anheben, committen
git tag -a v1.1 -m "Schreiben 1.1" -m "- Was sich geändert hat" && git push origin main v1.1
```

## Lizenz

Der Code steht unter der [MIT-Lizenz](LICENSE).
