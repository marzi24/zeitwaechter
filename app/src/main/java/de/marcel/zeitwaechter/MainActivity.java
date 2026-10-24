package de.marcel.zeitwaechter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Der einzige Bildschirm der App. */
public class MainActivity extends Activity {

    /** Wenn gesetzt, öffnet die App direkt die Uhrzeitauswahl für die Startzeit. */
    static final String EXTRA_EDIT_START = "edit_start";

    private static final int REQ_BASIC = 1;
    private static final int REQ_BACKGROUND = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshAll();
            handler.postDelayed(this, 15_000);
        }
    };

    private TextView tvStatus, tvDeadline, tvRemaining, tvWorked, tvPerms, tvGeofence, tvHistory;
    private Button btnStart, btnEditStart, btnEnd, btnDiscard, btnMinus, btnPlus;
    private EditText etLat, etLon, etRadius, etOffset, etMaxHours, etBreak, etWarn, etResume;
    private Switch swEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notifier.createChannels(this);

        tvStatus = findViewById(R.id.tvStatus);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvWorked = findViewById(R.id.tvWorked);
        tvPerms = findViewById(R.id.tvPerms);
        tvGeofence = findViewById(R.id.tvGeofence);
        tvHistory = findViewById(R.id.tvHistory);

        btnStart = findViewById(R.id.btnStart);
        btnEditStart = findViewById(R.id.btnEditStart);
        btnEnd = findViewById(R.id.btnEnd);
        btnDiscard = findViewById(R.id.btnDiscard);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);

        etLat = findViewById(R.id.etLat);
        etLon = findViewById(R.id.etLon);
        etRadius = findViewById(R.id.etRadius);
        etOffset = findViewById(R.id.etOffset);
        etMaxHours = findViewById(R.id.etMaxHours);
        etBreak = findViewById(R.id.etBreak);
        etWarn = findViewById(R.id.etWarn);
        etResume = findViewById(R.id.etResume);
        swEnabled = findViewById(R.id.swEnabled);

        fillSettingsFields();
        fillLocationFields();

        // ---- Heute ----
        btnStart.setOnClickListener(v -> {
            SessionManager.start(this, System.currentTimeMillis(), "manuell");
            toast("Eingestempelt um " + Fmt.time(System.currentTimeMillis()));
            refreshAll();
        });

        btnEditStart.setOnClickListener(v -> editStart());
        btnMinus.setOnClickListener(v -> shiftStart(-1));
        btnPlus.setOnClickListener(v -> shiftStart(+1));

        btnEnd.setOnClickListener(v -> {
            final long now = System.currentTimeMillis();
            new AlertDialog.Builder(this)
                    .setTitle("Ausgestempelt?")
                    .setMessage("Wann hast du ausgestempelt?")
                    .setPositiveButton("Jetzt (" + Fmt.time(now) + ")", (d, w) -> {
                        SessionManager.end(this, now, true);
                        refreshAll();
                    })
                    .setNeutralButton("Andere Uhrzeit …", (d, w) ->
                            pickTime(now, "Ausgestempelt um …", ms -> {
                                SessionManager.end(this, ms, true);
                                refreshAll();
                            }))
                    .setNegativeButton("Abbrechen", null)
                    .show();
        });

        btnDiscard.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Sitzung verwerfen?")
                .setMessage("Z. B. wenn du nur am Arbeitsort vorbeigefahren bist. Es wird nichts gespeichert.")
                .setPositiveButton("Verwerfen", (d, w) -> {
                    SessionManager.discard(this);
                    refreshAll();
                })
                .setNegativeButton("Abbrechen", null)
                .show());

        // ---- Einrichtung ----
        findViewById(R.id.btnPermLocation).setOnClickListener(v -> requestBasicPermissions());
        findViewById(R.id.btnPermBackground).setOnClickListener(v -> requestBackgroundLocation());
        findViewById(R.id.btnPermBattery).setOnClickListener(v -> requestBatteryAndAlarms());

        // ---- Arbeitsort ----
        findViewById(R.id.btnUseCurrent).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Aktuellen Standort speichern?")
                .setMessage("Mach das am besten, wenn du auf dem Parkplatz bei der Arbeit stehst. "
                        + "Der Kreis (Radius) wird um diesen Punkt gelegt.")
                .setPositiveButton("Speichern", (d, w) -> useCurrentLocation())
                .setNegativeButton("Abbrechen", null)
                .show());

        findViewById(R.id.btnOpenMap).setOnClickListener(v -> openMap());

        // ---- Einstellungen & Verlauf ----
        findViewById(R.id.btnSave).setOnClickListener(v -> saveSettings());
        findViewById(R.id.btnExport).setOnClickListener(v -> exportCsv());
        findViewById(R.id.btnEditLast).setOnClickListener(v -> editLastEntry());
        findViewById(R.id.btnDeleteLast).setOnClickListener(v -> deleteLastEntry());

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /** Aus der Benachrichtigung "Startzeit anpassen" geöffnet? Dann gleich die Uhrzeitauswahl zeigen. */
    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_EDIT_START, false)) {
            intent.removeExtra(EXTRA_EDIT_START);
            handler.post(this::editStart);
        }
    }

    // =====================================================================
    // Startzeit mit der Stempeluhr abgleichen
    // =====================================================================

    private void editStart() {
        long initial = Prefs.active(this) ? Prefs.start(this) : System.currentTimeMillis();
        pickTime(initial, "Laut Stempeluhr eingestempelt um …", ms -> {
            SessionManager.setStart(this, ms);
            toast("Startzeit: " + Fmt.timeSmart(ms) + " · spätestens raus: "
                    + Fmt.timeSmart(SessionManager.deadline(this, ms)));
            refreshAll();
        });
    }

    /** Startzeit um einzelne Minuten verschieben. */
    private void shiftStart(int minutes) {
        if (!Prefs.active(this)) return;
        long ms = Prefs.start(this) + minutes * 60_000L;
        SessionManager.setStart(this, ms);
        toast("Startzeit: " + Fmt.time(ms));
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Bei jedem Öffnen sicherheitshalber alles neu einrichten (z. B. nach "Stopp erzwingen").
        GeofenceHelper.register(this);
        if (Prefs.active(this)) {
            SessionManager.scheduleAlarms(this);
            Notifier.showStatus(this);
        }
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    // =====================================================================
    // Anzeige
    // =====================================================================

    private void refreshAll() {
        refreshStatus();
        refreshPerms();
        tvGeofence.setText("Ortserkennung: " + Prefs.geofenceStatus(this));
        refreshHistory();
    }

    private void refreshStatus() {
        boolean active = Prefs.active(this);
        long now = System.currentTimeMillis();

        if (active) {
            long start = Prefs.start(this);
            long dl = SessionManager.deadline(this, start);
            long left = dl - now;

            tvStatus.setText("Eingestempelt seit " + Fmt.timeSmart(start) + "  (" + Prefs.source(this) + ")");
            tvDeadline.setText("Spätestens raus: " + Fmt.timeSmart(dl));

            if (left > 0) {
                tvRemaining.setText("Noch " + Fmt.durationWords(left));
                int color;
                if (left > 60 * 60_000L) color = getColor(R.color.ok);
                else if (left > 30 * 60_000L) color = getColor(R.color.warn);
                else color = getColor(R.color.danger);
                tvRemaining.setTextColor(color);
            } else {
                tvRemaining.setText("Grenze überschritten seit " + Fmt.durationWords(-left) + "!");
                tvRemaining.setTextColor(getColor(R.color.danger));
            }

            long gross = Math.max(0, now - start);
            tvWorked.setText("Anwesend: " + Fmt.duration(gross)
                    + "  ·  netto ca. " + Fmt.duration(Fmt.netMs(gross))
                    + "\nGrenze = Start + " + Fmt.hoursFromMinutes(Prefs.maxWorkMin(this)) + " h + "
                    + Prefs.breakMin(this) + " Min. Pause");
        } else {
            tvStatus.setText("Nicht eingestempelt.");
            tvDeadline.setText("—");
            tvRemaining.setText(Prefs.trackingEnabled(this)
                    ? "Wartet auf Ankunft am Arbeitsort …"
                    : "Automatische Erkennung ist aus.");
            tvRemaining.setTextColor(getColor(R.color.text_secondary));
            tvWorked.setText("");
        }

        btnStart.setEnabled(!active);
        btnEnd.setEnabled(active);
        btnDiscard.setEnabled(active);
        btnMinus.setEnabled(active);
        btnPlus.setEnabled(active);
        btnEditStart.setText(active ? "Start " + Fmt.time(Prefs.start(this)) + " ändern" : "Startzeit festlegen");
    }

    private void refreshPerms() {
        String sb = mark(GeofenceHelper.hasFine(this)) + "  Standort (genau)\n"
                + mark(GeofenceHelper.hasBackground(this)) + "  Standort „Immer zulassen“\n"
                + mark(notificationsAllowed()) + "  Benachrichtigungen\n"
                + mark(batteryOk()) + "  Akku-Optimierung aus\n"
                + mark(SessionManager.canExact(this)) + "  Pünktliche Wecker\n"
                + mark(Prefs.hasWorkLocation(this)) + "  Arbeitsort gespeichert";
        tvPerms.setText(sb);
    }

    private void refreshHistory() {
        List<Prefs.Entry> list = Prefs.history(this);
        if (list.isEmpty()) {
            tvHistory.setText("Noch keine Einträge.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (int i = list.size() - 1; i >= 0 && shown < 30; i--, shown++) {
            Prefs.Entry e = list.get(i);
            long gross = e.end - e.start;
            sb.append(Fmt.day(e.start)).append("  ")
                    .append(Fmt.time(e.start)).append("–").append(Fmt.time(e.end))
                    .append("  ").append(Fmt.duration(gross))
                    .append(" (netto ").append(Fmt.duration(Fmt.netMs(gross))).append(")");
            if (i > 0) sb.append('\n');
        }
        tvHistory.setText(sb.toString());
    }

    private static String mark(boolean ok) {
        return ok ? "✅" : "❌";
    }

    private boolean notificationsAllowed() {
        return getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    private boolean batteryOk() {
        PowerManager pm = getSystemService(PowerManager.class);
        return pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    // =====================================================================
    // Berechtigungen
    // =====================================================================

    private void requestBasicPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS);
        requestPermissions(perms.toArray(new String[0]), REQ_BASIC);
    }

    private void requestBackgroundLocation() {
        if (!GeofenceHelper.hasFine(this)) {
            toast("Bitte zuerst Schritt 1 erledigen und „Genauer Standort“ erlauben.");
            return;
        }
        if (GeofenceHelper.hasBackground(this)) {
            toast("Ist schon erlaubt 👍");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Standort im Hintergrund")
                .setMessage("Damit die App merkt, wann du ankommst, obwohl sie geschlossen ist, "
                        + "wähle im nächsten Fenster bei „Standort“ die Option „Immer zulassen“.")
                .setPositiveButton("Weiter", (d, w) ->
                        requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                REQ_BACKGROUND))
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    @SuppressLint("BatteryLife")
    private void requestBatteryAndAlarms() {
        if (!batteryOk()) {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (ActivityNotFoundException e) {
                openAppSettings();
            }
            return;
        }
        if (!SessionManager.canExact(this) && Build.VERSION.SDK_INT >= 31) {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } catch (ActivityNotFoundException e) {
                openAppSettings();
            }
            return;
        }
        toast("Akku & Wecker sind schon richtig eingestellt 👍");
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BACKGROUND && !GeofenceHelper.hasBackground(this)) {
            toast("Bitte unter „Berechtigungen → Standort“ auf „Immer zulassen“ stellen.");
            openAppSettings();
        }
        if (requestCode == REQ_BASIC && !GeofenceHelper.hasFine(this)) {
            toast("Ohne genauen Standort kann der Arbeitsort nicht erkannt werden.");
        }
        GeofenceHelper.register(this);
        refreshAll();
    }

    // =====================================================================
    // Arbeitsort
    // =====================================================================

    @SuppressLint("MissingPermission")
    private void useCurrentLocation() {
        if (!GeofenceHelper.hasFine(this)) {
            toast("Bitte zuerst Schritt 1 (Standort erlauben).");
            return;
        }
        toast("Standort wird ermittelt …");
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, loc -> {
                    if (loc == null) {
                        toast("Kein Standort gefunden. Ist GPS an? Am besten draußen nochmal versuchen.");
                        return;
                    }
                    Prefs.setWorkLocation(this, loc.getLatitude(), loc.getLongitude());
                    fillLocationFields();
                    GeofenceHelper.register(this);
                    toast(String.format(Locale.GERMANY,
                            "Arbeitsort gespeichert (Genauigkeit ±%.0f m).", loc.getAccuracy()));
                    handler.postDelayed(this::refreshAll, 1500);
                })
                .addOnFailureListener(this, e -> toast("Fehler: " + e.getMessage()));
    }

    private void openMap() {
        if (!Prefs.hasWorkLocation(this)) {
            toast("Noch kein Arbeitsort gespeichert.");
            return;
        }
        String lat = Double.toString(Prefs.lat(this));
        String lon = Double.toString(Prefs.lon(this));
        Uri uri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(Arbeit)");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            toast("Keine Karten-App gefunden.");
        }
    }

    private void fillLocationFields() {
        if (Prefs.hasWorkLocation(this)) {
            etLat.setText(String.format(Locale.US, "%.6f", Prefs.lat(this)));
            etLon.setText(String.format(Locale.US, "%.6f", Prefs.lon(this)));
        } else {
            etLat.setText("");
            etLon.setText("");
        }
    }

    // =====================================================================
    // Einstellungen
    // =====================================================================

    private void fillSettingsFields() {
        swEnabled.setChecked(Prefs.trackingEnabled(this));
        etRadius.setText(String.valueOf(Prefs.radius(this)));
        etOffset.setText(String.valueOf(Prefs.offsetMin(this)));
        etMaxHours.setText(Fmt.hoursFromMinutes(Prefs.maxWorkMin(this)));
        etBreak.setText(String.valueOf(Prefs.breakMin(this)));
        etWarn.setText(Prefs.warnList(this));
        etResume.setText(String.valueOf(Prefs.resumeMin(this)));
    }

    private void saveSettings() {
        try {
            // Koordinaten: entweder zwei Felder oder "48.49, 9.21" ins erste Feld eingefügt
            String latS = etLat.getText().toString().trim();
            String lonS = etLon.getText().toString().trim();
            if (latS.matches("-?\\d+\\.\\d+\\s*,\\s*-?\\d+\\.\\d+")) {
                String[] p = latS.split(",");
                latS = p[0].trim();
                lonS = p[1].trim();
            }
            boolean hasCoords = !latS.isEmpty() || !lonS.isEmpty();
            double lat = 0, lon = 0;
            if (hasCoords) {
                lat = parseDouble(latS, "Breitengrad");
                lon = parseDouble(lonS, "Längengrad");
                if (lat < -90 || lat > 90) throw new IllegalArgumentException("Breitengrad muss zwischen -90 und 90 liegen.");
                if (lon < -180 || lon > 180) throw new IllegalArgumentException("Längengrad muss zwischen -180 und 180 liegen.");
            }

            int radius = parseInt(etRadius, "Radius", 50, 2000);
            int offset = parseInt(etOffset, "Minuten bis zur Stempeluhr", 0, 30);
            double maxHours = parseDouble(etMaxHours.getText().toString(), "Höchstarbeitszeit");
            if (maxHours < 1 || maxHours > 16) throw new IllegalArgumentException("Höchstarbeitszeit: 1 bis 16 Stunden.");
            int maxWork = (int) Math.round(maxHours * 60);
            int brk = parseInt(etBreak, "Pause", 0, 180);
            int resume = parseInt(etResume, "Fortsetzen-Zeit", 0, 240);
            String warn = etWarn.getText().toString().trim();
            if (Prefs.parseWarnList(warn).isEmpty() && !warn.isEmpty()) {
                throw new IllegalArgumentException("Warnungen: Zahlen mit Komma, z. B. 30, 15, 5");
            }

            Prefs.saveSettings(this, radius, offset, maxWork, brk, resume, warn, swEnabled.isChecked());
            if (hasCoords) {
                Prefs.setWorkLocation(this, lat, lon);
            } else {
                Prefs.clearWorkLocation(this);
            }
        } catch (IllegalArgumentException e) {
            toast(e.getMessage());
            return;
        }

        GeofenceHelper.register(this);
        if (Prefs.active(this)) {
            SessionManager.scheduleAlarms(this);
            Notifier.showStatus(this);
        }
        fillSettingsFields();
        fillLocationFields();
        toast("Gespeichert ✔");
        handler.postDelayed(this::refreshAll, 1500);
        refreshAll();
    }

    private static double parseDouble(String text, String name) {
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + ": bitte eine Zahl eingeben.");
        }
    }

    private static int parseInt(EditText field, String name, int min, int max) {
        int v;
        try {
            v = Integer.parseInt(field.getText().toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + ": bitte eine ganze Zahl eingeben.");
        }
        if (v < min || v > max) {
            throw new IllegalArgumentException(name + ": erlaubt ist " + min + " bis " + max + ".");
        }
        return v;
    }

    // =====================================================================
    // Verlauf
    // =====================================================================

    private void exportCsv() {
        List<Prefs.Entry> list = Prefs.history(this);
        if (list.isEmpty()) {
            toast("Noch keine Einträge.");
            return;
        }
        StringBuilder csv = new StringBuilder("Datum;Kommen (geschätzt);Gehen (geschätzt);Anwesenheit h;Netto h (geschätzt);Quelle\n");
        for (Prefs.Entry e : list) {
            long gross = e.end - e.start;
            csv.append(Fmt.date(e.start)).append(';')
                    .append(Fmt.time(e.start)).append(';')
                    .append(Fmt.time(e.end)).append(';')
                    .append(Fmt.hoursDecimal(gross)).append(';')
                    .append(Fmt.hoursDecimal(Fmt.netMs(gross))).append(';')
                    .append(e.source).append('\n');
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Zeitwächter Verlauf");
        send.putExtra(Intent.EXTRA_TEXT, csv.toString());
        startActivity(Intent.createChooser(send, "Verlauf teilen"));
    }

    /** Beginn oder Ende des letzten Verlaufseintrags nachträglich ändern. */
    private void editLastEntry() {
        List<Prefs.Entry> list = Prefs.history(this);
        if (list.isEmpty()) {
            toast("Noch keine Einträge.");
            return;
        }
        Prefs.Entry last = list.get(list.size() - 1);
        new AlertDialog.Builder(this)
                .setTitle("Letzten Eintrag bearbeiten")
                .setMessage(Fmt.day(last.start) + "  " + Fmt.time(last.start) + "–" + Fmt.time(last.end)
                        + "\nWas möchtest du ändern?")
                .setPositiveButton("Beginn", (d, w) ->
                        pickTimeOnDay(last.start, "Beginn laut Stempeluhr …", ms -> {
                            long start = ms;
                            if (start > last.end) start -= 24 * 3_600_000L; // Nachtdienst: Beginn am Vortag
                            Prefs.replaceLastHistory(this, new Prefs.Entry(start, last.end, "korrigiert"));
                            toast("Beginn geändert auf " + Fmt.timeSmart(start));
                            refreshAll();
                        }))
                .setNeutralButton("Ende", (d, w) ->
                        pickTimeOnDay(last.end, "Ende laut Stempeluhr …", ms -> {
                            long end = ms;
                            if (end < last.start) end += 24 * 3_600_000L; // Nachtdienst: Ende am Folgetag
                            Prefs.replaceLastHistory(this, new Prefs.Entry(last.start, end, "korrigiert"));
                            toast("Ende geändert auf " + Fmt.timeSmart(end));
                            refreshAll();
                        }))
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void deleteLastEntry() {
        List<Prefs.Entry> list = Prefs.history(this);
        if (list.isEmpty()) {
            toast("Noch keine Einträge.");
            return;
        }
        Prefs.Entry last = list.get(list.size() - 1);
        new AlertDialog.Builder(this)
                .setTitle("Letzten Eintrag löschen?")
                .setMessage(Fmt.day(last.start) + "  " + Fmt.time(last.start) + "–" + Fmt.time(last.end))
                .setPositiveButton("Löschen", (d, w) -> {
                    Prefs.popLastHistory(this);
                    refreshAll();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    // =====================================================================
    // Hilfsfunktionen
    // =====================================================================

    private interface TimeCallback {
        void onTime(long ms);
    }

    /** Uhrzeit wählen. Liegt die Zeit in der Zukunft, ist "gestern" gemeint (Nachtdienst). */
    private void pickTime(long initialMs, String title, TimeCallback cb) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime init = LocalDateTime.ofInstant(Instant.ofEpochMilli(initialMs), zone);
        TimePickerDialog d = new TimePickerDialog(this, (view, hour, minute) -> {
            LocalDate day = LocalDate.now(zone);
            long ms = day.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli();
            if (ms > System.currentTimeMillis() + 60_000L) {
                ms = day.minusDays(1).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli();
            }
            cb.onTime(ms);
        }, init.getHour(), init.getMinute(), true);
        d.setTitle(title);
        d.show();
    }

    /** Uhrzeit wählen, und zwar am selben Tag wie baseMs (für Verlaufseinträge). */
    private void pickTimeOnDay(long baseMs, String title, TimeCallback cb) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime base = LocalDateTime.ofInstant(Instant.ofEpochMilli(baseMs), zone);
        TimePickerDialog d = new TimePickerDialog(this, (view, hour, minute) ->
                cb.onTime(base.toLocalDate().atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()),
                base.getHour(), base.getMinute(), true);
        d.setTitle(title);
        d.show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
