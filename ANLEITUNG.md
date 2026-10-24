# Zeitwächter – Anleitung

Eine kleine Android-App, die dich **rechtzeitig vor der Höchstarbeitszeit ans Ausstempeln erinnert**.
Sie erkennt per Standort, wann du bei der Arbeit ankommst, schätzt daraus deine Einstempelzeit und
warnt dich vor der Grenze. Die offizielle Zeit kommt weiterhin von der Stempeluhr – die App ist nur
deine private Erinnerungshilfe. Alle Daten bleiben auf dem Handy.

---

## 1. Was die App macht

| Situation | Was passiert |
|---|---|
| Du fährst auf den Parkplatz (Kreis um den Arbeitsort) | Die App merkt sich den Zeitpunkt und rechnet **+ 2 Minuten** (Parken + Laufen zur Stempeluhr) = geschätzte Einstempelzeit |
| Während der Arbeit | Dauerhafte Benachrichtigung mit **Countdown** bis zur spätesten Ausstempelzeit |
| 30, 15 und 5 Min. vor der Grenze | **Warnung** mit Vibration (laut, auch im Energiesparmodus) |
| Grenze erreicht | Letzte Warnung „jetzt ausstempeln!“ |
| Du verlässt den Arbeitsort | Sitzung endet automatisch (Gehen-Zeit = Verlassen − 2 Min.) und landet im Verlauf |

**So wird die späteste Ausstempelzeit berechnet:**

```
späteste Ausstempelzeit = geschätzte Einstempelzeit + 10 h Arbeit + 45 Min. Pause
Beispiel:  Ankunft Parkplatz 06:28  →  eingestempelt ca. 06:30  →  spätestens raus 17:15
```

Alle Werte (2 Min., 10 h, 45 Min., Warnzeiten) kannst du in der App ändern.

**Gut zu wissen:**
- Funktioniert auch über Mitternacht (Nachtdienst): Dann steht z. B. „09:01 (So. 25.10.)“ da.
- Bist du kurz weg (z. B. Mittag außerhalb) und innerhalb von 45 Min. wieder da, läuft die alte
  Sitzung mit der **ursprünglichen** Startzeit weiter. Die Grenze wird also nie nach hinten verschoben.
- Die Geofence-Meldung kann ein paar Minuten verspätet kommen. Die App nimmt deshalb den Zeitstempel,
  an dem das Handy wirklich die Grenze überquert hat, nicht den Zeitpunkt der Meldung.

---

## 2. Die APK erzeugen

Du hast zwei Möglichkeiten. **Weg A (GitHub)** ist der einfachste, weil du nichts installieren musst.

### Weg A: Automatisch über GitHub (empfohlen, ca. 10 Minuten)

GitHub hat Rechner, die die App für dich bauen. Das ist kostenlos.

1. **Konto anlegen:** Auf <https://github.com> registrieren (falls noch nicht vorhanden).
2. **Neues Repository:** Oben rechts auf **„+“ → „New repository“**.
   - Name: `zeitwaechter`
   - **Private** auswählen (dann sieht es niemand außer dir)
   - Auf **„Create repository“** klicken.
3. **Dateien hochladen:**
   - ZIP-Datei `Zeitwaechter.zip` auf dem PC entpacken.
   - Auf der GitHub-Seite des neuen Repositorys auf **„uploading an existing file“** klicken.
   - Den **Inhalt** des entpackten Ordners `Zeitwaechter` (also `app`, `gradle`, `gradlew`, `build.gradle` usw.)
     ins Browserfenster ziehen. Nicht den Ordner selbst, sondern was darin liegt.
   - Unten auf **„Commit changes“** klicken.
4. **Prüfen, ob der Bau-Auftrag mit hochgeladen wurde:** Im Repository muss ein Ordner `.github` zu sehen sein.
   - Windows zeigt Ordner mit Punkt am Anfang normal an; auf dem Mac sind sie im Finder versteckt
     (mit **Cmd + Shift + .** sichtbar machen).
   - Fehlt `.github`: Reiter **„Actions“** → **„set up a workflow yourself“** → den Inhalt der Datei
     `.github/workflows/apk-bauen.yml` hineinkopieren → **„Commit changes“**.
5. **Bauen lassen:** Reiter **„Actions“** öffnen. Dort läuft „APK bauen“ (gelber Punkt = läuft, ca. 3–5 Min.).
   Läuft nichts: links „APK bauen“ anklicken → **„Run workflow“**.
6. **APK herunterladen:** Wenn der Punkt **grün** ist, den Lauf anklicken → ganz unten bei **„Artifacts“**
   auf **„Zeitwaechter-APK“** klicken. Du bekommst eine ZIP, darin liegt `app-debug.apk`.

> Roter Punkt = Fehler. Den Lauf anklicken, den roten Schritt aufklappen und mir den Fehlertext schicken.

### Weg B: Mit Android Studio am PC

1. **Android Studio** installieren: <https://developer.android.com/studio> (kostenlos, ca. 3 GB).
   Beim ersten Start einfach den Standard-Einstellungen folgen.
2. `Zeitwaechter.zip` entpacken.
3. In Android Studio: **File → Open** → den Ordner `Zeitwaechter` auswählen → **„Trust Project“**.
4. Warten, bis unten die Leiste „Gradle sync“ fertig ist (beim ersten Mal 5–10 Min., lädt Dinge aus dem Internet).
   - Fragt Android Studio nach einem Update von „Android Gradle Plugin“: einfach zustimmen.
   - Fehlt eine „SDK Platform“: auf den blauen Link **„Install …“** in der Fehlermeldung klicken.
5. Menü **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
6. Unten rechts erscheint „APK(s) generated“ → auf **„locate“** klicken.
   Die Datei heißt `app-debug.apk` (Ordner `app/build/outputs/apk/debug/`).

**Noch einfacher:** Handy per USB anschließen, auf dem Handy die Entwickleroptionen und
„USB-Debugging“ aktivieren, dann in Android Studio auf den grünen ▶-Knopf. Die App wird direkt installiert.

---

## 3. Auf dem Handy installieren

1. `app-debug.apk` aufs Handy bringen (per USB-Kabel, Google Drive, E-Mail an dich selbst o. ä.).
2. Die Datei auf dem Handy antippen (z. B. in der App „Dateien“ unter „Downloads“).
3. Android fragt: **„Installation aus unbekannten Quellen erlauben?“** → für diese App (Dateien/Chrome)
   **erlauben** → zurück → **„Installieren“**.
4. Meldet Google Play Protect „Unbekannte App“: **„Weitere Details“ → „Trotzdem installieren“**.
   Das kommt, weil die App nicht aus dem Play Store stammt.

---

## 4. Die App einrichten (einmalig, ca. 3 Minuten)

Öffne die App. Im Bereich **„Einrichtung“** siehst du ✅ und ❌. Gehe die drei Knöpfe der Reihe nach durch:

**Knopf 1 – „Standort & Benachrichtigungen erlauben“**
- Standort: **„Genau“** auswählen und **„Bei Nutzung der App“** tippen.
- Benachrichtigungen: **„Zulassen“**.

**Knopf 2 – „Standort ‚Immer zulassen‘“**
- Es öffnet sich eine Einstellungsseite. Dort **„Immer zulassen“** auswählen.
- Ohne das merkt die App nicht, wenn du ankommst, während sie geschlossen ist.

**Knopf 3 – „Akku-Optimierung & Wecker“**
- Bei der Frage nach der Akku-Optimierung **„Zulassen“** tippen.
- Kommt danach eine Seite „Wecker und Erinnerungen“: Schalter **an**.
- Die App verbraucht dadurch kaum mehr Akku. Geofencing nutzt die Ortung, die Android sowieso macht.

**Arbeitsort speichern**
- Am einfachsten: Stell dich **auf den Parkplatz bei der Arbeit** und tippe
  **„Aktuellen Standort als Arbeitsort speichern“**.
- Oder von zu Hause: In Google Maps lange auf deinen Parkplatz drücken → oben erscheinen Zahlen wie
  `48.49123, 9.20432` → kopieren → in das Feld **„Breitengrad“** einfügen → **„Speichern“**.
  Die App teilt die beiden Zahlen automatisch auf.
- Mit **„Ort in Karte prüfen“** kontrollierst du, ob der Punkt stimmt.

**Einstellungen** (Standardwerte passen meist):

| Feld | Standard | Erklärung |
|---|---|---|
| Radius | 150 m | Größe des Kreises. Sollte den Parkplatz umfassen. 100–300 m ist gut. Zu klein = unzuverlässig. |
| Minuten bis zur Stempeluhr | 2 | Parken + Laufen. Lieber etwas **knapp** wählen, dann kommt die Warnung eher zu früh als zu spät. |
| Höchstarbeitszeit | 10 | In Stunden, ohne Pause. Auch Kommazahlen, z. B. `9,5`. |
| Pause | 45 | Minuten, die zusätzlich eingerechnet werden. |
| Warnungen | 30, 15, 5 | Wann vor der Grenze gewarnt wird. Beliebig viele, mit Komma. |
| Fortsetzen innerhalb von | 45 | Kommst du so schnell zurück, läuft die alte Sitzung weiter. |

Danach **„Speichern“**. Bei „Ortserkennung“ sollte **„Aktiv (Radius … m)“** stehen. Alle Punkte bei
„Einrichtung“ sollten ✅ zeigen.

---

## 5. Herstellerspezifische Akku-Einstellungen (wichtig!)

Manche Hersteller beenden Apps im Hintergrund trotzdem. Dann kommen Warnungen zu spät oder gar nicht.

- **Samsung:** Einstellungen → Akku → Hintergrund-Nutzungslimits → **„Nie inaktive Apps“** → Zeitwächter hinzufügen.
- **Xiaomi / Redmi / POCO:** App-Info → **Autostart an**, Akku-Sparmodus → **„Keine Einschränkungen“**.
- **Huawei / Honor:** Einstellungen → Akku → App-Start → Zeitwächter → **manuell verwalten, alles an**.
- **OnePlus / Oppo / Realme:** App-Info → Akkunutzung → **„Hintergrundaktivität zulassen“**.
- **Google Pixel:** Mit Knopf 3 ist alles erledigt.

Genaue Anleitungen pro Modell findest du auf <https://dontkillmyapp.com>.

---

## 6. Im Alltag

- **Normalfall:** Nichts tun. Ankommen → Meldung „Angekommen – eingestempelt ca. 06:30“.
  Gehen → Meldung „Feierabend erkannt“.
- **Mit der Stempeluhr abgleichen:** Nach dem Einstempeln die Zeit auf der Stempeluhr ablesen und
  in der Ankunfts- oder Countdown-Benachrichtigung auf **„Startzeit anpassen“** tippen. Es öffnet sich
  direkt die Uhrzeitauswahl. In der App geht es über den blauen Knopf **„Start 06:30 ändern“**
  oder für kleine Abweichungen über **−1 Min / +1 Min**. Die Warnungen werden sofort neu berechnet.
  Ab dann rechnet die App mit der exakten Zeit der Stempeluhr.
- **Nachträglich korrigieren:** Im Verlauf **„Letzten Eintrag bearbeiten“** → „Beginn“ oder „Ende“
  wählen und die Zeit laut Stempeluhr eintragen. Funktioniert auch bei Nachtdiensten.
- **Nur vorbeigefahren / kein Arbeitstag:** In der Benachrichtigung **„Kein Arbeitstag“** tippen.
- **Erkennung hat nicht geklappt:** **„Jetzt eingestempelt“** oder **„Startzeit festlegen“** tippen.
- **Früher ausgestempelt:** In der Benachrichtigung **„Ausgestempelt“** tippen, dann kommen keine Warnungen mehr.
- **Verlauf:** Unten in der App, mit geschätzter Netto-Zeit (nach § 4 ArbZG: über 6 h → 30 Min.,
  über 9 h → 45 Min. Pause abgezogen). Mit **„Als CSV teilen“** kannst du ihn dir z. B. per Mail schicken
  und in Excel öffnen.

---

## 7. Erster Test

Mach den ersten Arbeitstag zur Probe:
1. Morgens prüfen, ob die Meldung „Angekommen“ kommt und die Uhrzeit ungefähr passt.
2. Die Countdown-Benachrichtigung sollte die ganze Zeit sichtbar sein.
3. Zum Testen der Warnungen in der App die **Startzeit** auf „jetzt minus 10 Std. 25 Min.“ setzen
   (z. B. jetzt 17:00 → Startzeit 06:35 → Grenze 17:20). Nach ca. 5 Min. kommt die 15-Min.-Warnung,
   um 17:15 die 5-Min.-Warnung.
   Danach die Startzeit wieder korrigieren oder „Verwerfen“.

---

## 8. Wenn etwas nicht klappt

| Problem | Lösung |
|---|---|
| Keine „Angekommen“-Meldung | Standort auf „Immer zulassen“? Handy-Standort (GPS) an? Akku-Einstellungen aus Abschnitt 5 prüfen. Radius vergrößern (z. B. 250 m). |
| „Fehler: Code 1000“ bei Ortserkennung | Der Handy-Standort ist aus oder „Immer zulassen“ fehlt. Einschalten und App einmal öffnen. |
| Meldung kommt erst im Gebäude | Radius vergrößern oder Arbeitsort-Punkt näher an die Einfahrt legen. |
| Sitzung endet mitten am Tag | GPS springt im Gebäude. Radius vergrößern. Kommt das Handy innerhalb von 45 Min. „zurück“, wird die Sitzung ohnehin fortgesetzt. |
| Warnung kommt nicht | Knopf 3 erledigt? Benachrichtigungen für „Warnungen vor der Höchstarbeitszeit“ in den Android-Einstellungen nicht stummgeschaltet? |
| Nach Neustart nichts mehr | App einmal öffnen. Normalerweise richtet sie sich nach dem Neustart selbst wieder ein. |

---

## 9. Projektaufbau (für Neugierige)

```
Zeitwaechter/
├─ app/src/main/
│  ├─ AndroidManifest.xml       Berechtigungen und Bausteine der App
│  ├─ java/de/marcel/zeitwaechter/
│  │  ├─ MainActivity.java      Der Bildschirm (Knöpfe, Anzeige, Einstellungen)
│  │  ├─ SessionManager.java    Die Logik: Start, Ende, Grenze berechnen, Warnungen planen
│  │  ├─ GeofenceHelper.java    Richtet den Kreis um den Arbeitsort ein
│  │  ├─ GeofenceReceiver.java  Wird beim Betreten/Verlassen aufgerufen
│  │  ├─ AlarmReceiver.java     Wird zu den Warnzeiten aufgerufen
│  │  ├─ ActionReceiver.java    Knöpfe in den Benachrichtigungen
│  │  ├─ BootReceiver.java      Richtet nach Neustart alles wieder ein
│  │  ├─ Notifier.java          Alle Benachrichtigungen
│  │  ├─ Prefs.java             Speichert Einstellungen und Verlauf auf dem Handy
│  │  └─ Fmt.java               Zeit-Formatierung, Netto-Berechnung
│  └─ res/                      Layout, Farben, Icons
├─ .github/workflows/apk-bauen.yml   Bauauftrag für GitHub
└─ build.gradle, settings.gradle, gradlew …   Bau-Konfiguration
```

Willst du später etwas ändern (z. B. andere Texte), reicht es, die `.java`-Datei anzupassen und neu
hochzuladen. GitHub baut dann automatisch eine neue APK. Beim Installieren über die alte App bleiben
Einstellungen und Verlauf erhalten.
