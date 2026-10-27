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
| Du verlässt den Arbeitsort | Sitzung endet automatisch (Gehen-Zeit = Verlassen − eingestellte Minuten, Standard 2) und landet im Verlauf |

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

Öffne die App und tippe oben rechts auf das **Zahnrad ⚙**. Dort sind **alle Einstellungen** an einem Ort.
Ganz oben im Bereich **„Einrichtung“** siehst du ✅ und ❌. Gehe die drei Knöpfe der Reihe nach durch.
Solange etwas fehlt, zeigt der Reiter „Heute“ einen gelben Hinweis, der direkt hierher führt.

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
  `48.49123, 9.20432` → **antippen** (damit sind sie kopiert) → in der App auf
  **„📋 Aus Zwischenablage einfügen“** tippen. Fertig, der Ort ist sofort gespeichert.
  Ein kopierter Google-Maps-Link aus dem Browser funktioniert auch, Kurz-Links (maps.app.goo.gl) aber nicht.
- Von Hand eintippen geht natürlich auch: Zahlen in die beiden Felder schreiben und auf
  **„✔ Arbeitsort übernehmen“** tippen (oder auf der Tastatur auf „Fertig“).
- Mit **„In Karte prüfen“** kontrollierst du, ob der Punkt stimmt.

**Erkennung & Erinnerungen** (Standardwerte passen meist):

| Feld | Standard | Erklärung |
|---|---|---|
| Radius | 150 m | Größe des Kreises. Sollte den Parkplatz umfassen. 100–300 m ist gut. Zu klein = unzuverlässig. |
| Morgens: Ankommen → Einstempeln | 2 | Parken + Laufen zur Stempeluhr. Lieber etwas **knapp** wählen, dann kommt die Warnung eher zu früh als zu spät. |
| Abends: Ausstempeln → Wegfahren | 2 | Laufen zum Auto + Losfahren. Wird beim Verlassen des Arbeitsorts von der Uhrzeit abgezogen, damit die Gehen-Zeit zur Stempeluhr passt. |
| Höchstarbeitszeit | 10 | In Stunden, ohne Pause. Auch Kommazahlen, z. B. `9,5`. |
| Warnungen | 30, 15, 5 | Wann vor der Grenze gewarnt wird. Beliebig viele, mit Komma. |
| Fortsetzen innerhalb von | 45 | Kommst du so schnell zurück, läuft die alte Sitzung weiter. |

Darunter folgen **Arbeitszeit & Überstunden**, **Zuschläge** und **Feiertage** (siehe Abschnitt 6).
Ganz unten auf **„Alle Einstellungen speichern“** tippen. Bei „Ortserkennung“ sollte
**„Aktiv (Radius … m)“** stehen, und alle Punkte bei „Einrichtung“ sollten ✅ zeigen.
Mit **✕** oben rechts oder der Zurück-Taste schließt du das Menü wieder.

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
- **Nachträglich korrigieren:** Im Reiter **„Arbeitszeit“** die Zeile antippen und Kommen, Gehen oder
  Pause laut Stempeluhr ändern. Funktioniert auch bei Nachtdiensten.
- **Zweimal am Tag arbeiten:** Z. B. bis 16:00 gearbeitet und abends um 20:00 nochmal: einfach wieder
  einstempeln (automatisch per Standort oder mit **„Erneut einstempeln“**). Die App rechnet die
  früheren Einsätze desselben Arbeitstags auf die 10-h-Grenze an, und die Zeit dazwischen zählt als Pause.
  Beispiel: 06:30–16:00 = 9:30 h anwesend − 45 Min. Pause = 8:45 h netto → um 20:00 eingestempelt → **spätestens 21:15 raus**.
  Als „selber Arbeitstag“ gilt alles, solange dazwischen keine 11 Stunden Ruhezeit lagen.
- **Nur vorbeigefahren / kein Arbeitstag:** In der Benachrichtigung **„Kein Arbeitstag“** tippen.
- **Erkennung hat nicht geklappt:** **„Jetzt eingestempelt“** oder **„Startzeit festlegen“** tippen.
- **Früher ausgestempelt:** In der Benachrichtigung **„Ausgestempelt“** tippen, dann kommen keine Warnungen mehr.
- **Dark-Modus:** Die App folgt automatisch der Handy-Einstellung (Einstellungen → Display → Dunkles Design).

### Reiter „Arbeitszeit“: Tabelle und Überstunden

**Einmalig einrichten** (⚙ → „Arbeitszeit & Überstunden“, „Zuschläge“, „Feiertage“):

| Feld | Bedeutung |
|---|---|
| Sollarbeitszeit pro Tag Mo–Fr | z. B. 38,5 h/Woche ÷ 5 Tage = **7,7**. Samstag, Sonntag und Feiertage haben **keine** Sollzeit – dort ist jede Stunde eine Überstunde. |
| Pause wird abgezogen ab | **6** Stunden Anwesenheit |
| Pause höchstens | **45** Minuten |
| Überstunden zum Start | Dein aktueller Überstundenstand laut Arbeitgeber, z. B. **35** (auch negativ möglich) |
| Überstunden-Stand gilt ab | Der Tag, ab dem die App rechnet. Einträge davor zählen nicht zum Konto. |
| Auszahlung am Monatsende ab | **40** – alles darüber wird am Monatsende ausgezahlt |
| Stundenlohn brutto | z. B. **25,50** |
| Zuschläge | Mo–Fr **25 %**, Samstag erste **2** h **25 %**, danach **50 %**, Sonntag **70 %**, Feiertag **100 %** |
| Bundesland | Für die gesetzlichen Feiertage (Standard Baden-Württemberg). Darunter siehst du die Liste fürs aktuelle Jahr. |
| Zusätzliche Feiertage | z. B. `24.12, 31.12`, falls dein Arbeitgeber diese Tage wie Feiertage bezuschlagt |

**Die Tabelle** zeigt pro Monat (mit ◀ ▶ blättern):

| Spalte | Bedeutung |
|---|---|
| Datum | Arbeitstag (bei zwei Einsätzen am Tag zwei Zeilen). **Farbig** = Samstag/Sonntag, **F** = Feiertag |
| Zeit | Kommen–Gehen, ⁺¹ = Ende am nächsten Tag |
| Pause | Mit * = automatisch (siehe unten), ohne * = von dir eingetragen |
| Netto | Arbeitszeit ohne Pause |
| +/− | Überstunden des Tages (Netto − Soll), grün = Plus, rot = Minus |

- **Zeile antippen** → Kommen, Gehen, Pause ändern oder Eintrag löschen.
- **„+ Eintrag“** → vergessenen Arbeitstag nachtragen.
- **„+ Freier Tag (Abbau)“** → Tag, an dem du Überstunden abfeierst. Zählt die Sollzeit als Minus.
- Tage **ohne** Eintrag (frei laut Dienstplan, Urlaub, krank) zählen weder Soll noch Ist.

**So wird die Pause abgezogen** (wie bei deinem Arbeitgeber, einstellbar unter ⚙):

| Anwesend | Pause | Netto |
|---|---|---|
| bis 6:00 h | 0 Min. | wie anwesend |
| 6:32 h | 32 Min. | 6:00 h |
| ab 6:45 h | 45 Min. | anwesend − 45 Min. |

Es wird also nie mehr abgezogen, als man über 6 h liegt. Gilt pro Einsatz; bei einem zweiten Einsatz
am selben Tag zählt die Lücke dazwischen als Pause.

**Überstundenkonto – so rechnet die App jeden Monat:**

```
Kontostand Monatsanfang          +40:00 h
+ Überstunden des Monats          +7:30 h
= Kontostand Monatsende          +47:30 h
→ über 40 h wird ausgezahlt        7:30 h
→ nächster Monat startet mit      +40:00 h
```

Liegt das Konto am Monatsende unter 40 h, wird nichts ausgezahlt und der Stand so übernommen.
Im ersten Monat startet das Konto mit deinen **„Überstunden zum Start“** ab dem eingestellten Tag.

**Zuschläge:** Die App teilt die Überstunden jedes Tages nach Art auf:

| Tag | Zuschlag (Standard) |
|---|---|
| Montag – Freitag | 25 % |
| Samstag, die ersten 2 Überstunden | 25 % |
| Samstag, ab der 3. Überstunde | 50 % |
| Sonntag | 70 % |
| Feiertag (geht vor Sa/So) | 100 % |

Unter dem Kontostand siehst du die Aufteilung des Monats und den **Durchschnittszuschlag**.
Die ausgezahlten Stunden werden mit diesem Durchschnitt bewertet, also jede Stunde anteilig so,
wie die Überstunden in diesem Monat entstanden sind:

```
Beispiel Oktober: 4 h Mo–Fr (25 %), 2 h Sa (25 %), 2 h Sa (50 %), 2 h So (70 %), 1 h Feiertag (100 %)
                  = 11 Überstunden, Ø 44,55 % Zuschlag
Konto 35 + 11 = 46 h  →  6 h über 40 h
6 h × 25,00 € × 1,4455 = 216,82 € brutto
```

Ein Arbeitstag zählt komplett zum Datum, an dem er begonnen hat (wichtig bei Nachtdiensten über Mitternacht).
**Ganz unten** im Reiter steht der Betrag groß auf einer grünen Karte: im laufenden Monat als
„Voraussichtliche Auszahlung“ (Stand heute), in vergangenen Monaten als endgültiger Wert.
Alle Beträge sind **brutto**, vor Steuern und Sozialabgaben.

Mit **„Alle Einträge als CSV teilen“** schickst du dir alles z. B. per Mail und öffnest es in Excel.

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
│  │  ├─ MainActivity.java      Der Bildschirm mit Reiter „Heute“ (Knöpfe, Anzeige, Einstellungen)
│  │  ├─ OvertimeTab.java       Reiter „Arbeitszeit“: Tabelle, Monatskonto, Zuschläge, Auszahlung
│  │  ├─ SettingsTab.java       Einstellungsmenü (Zahnrad): alle Einstellungen an einem Ort
│  │  ├─ Work.java              Rechnungen: Tagesgrenze, Überstunden, Zuschläge, Monatskonto
│  │  ├─ Holidays.java          Gesetzliche Feiertage je Bundesland (inkl. Ostern-Berechnung)
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
