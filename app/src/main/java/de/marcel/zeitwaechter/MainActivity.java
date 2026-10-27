package de.marcel.zeitwaechter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Der Bildschirm der App mit den Reitern „Heute“ und „Arbeitszeit“
 * und dem Einstellungsmenü hinter dem Zahnrad.
 */
public class MainActivity extends Activity {

    /** Wenn gesetzt, öffnet die App direkt die Uhrzeitauswahl für die Startzeit. */
    static final String EXTRA_EDIT_START = "edit_start";

    private static final String STATE_TAB = "tab";
    static final int TAB_HEUTE = 0;
    static final int TAB_ZEIT = 1;
    static final int TAB_SETTINGS = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshAll();
            handler.postDelayed(this, 15_000);
        }
    };

    private TextView tvStatus, tvDeadline, tvRemaining, tvWorked, tvSetupHint, tvTitle, btnSettings;
    private TextView tabHeute, tabZeit;
    private View tabBar, scrollHeute, scrollZeit, scrollSettings;
    private Button btnStart, btnEditStart, btnEnd, btnDiscard, btnMinus, btnPlus;
    private OvertimeTab overtime;
    private SettingsTab settings;
    private int currentTab = TAB_HEUTE;
    private int lastMainTab = TAB_HEUTE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notifier.createChannels(this);

        tvStatus = findViewById(R.id.tvStatus);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvWorked = findViewById(R.id.tvWorked);
        tvSetupHint = findViewById(R.id.tvSetupHint);
        tvTitle = findViewById(R.id.tvTitle);
        btnSettings = findViewById(R.id.btnSettings);

        btnStart = findViewById(R.id.btnStart);
        btnEditStart = findViewById(R.id.btnEditStart);
        btnEnd = findViewById(R.id.btnEnd);
        btnDiscard = findViewById(R.id.btnDiscard);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);

        // ---- Reiter & Einstellungsmenü ----
        tabBar = findViewById(R.id.tabBar);
        tabHeute = findViewById(R.id.tabHeute);
        tabZeit = findViewById(R.id.tabZeit);
        scrollHeute = findViewById(R.id.scrollHeute);
        scrollZeit = findViewById(R.id.scrollZeit);
        scrollSettings = findViewById(R.id.scrollSettings);
        overtime = new OvertimeTab(this);
        settings = new SettingsTab(this);
        tabHeute.setOnClickListener(v -> showTab(TAB_HEUTE));
        tabZeit.setOnClickListener(v -> showTab(TAB_ZEIT));
        btnSettings.setOnClickListener(v -> showTab(currentTab == TAB_SETTINGS ? lastMainTab : TAB_SETTINGS));
        tvSetupHint.setOnClickListener(v -> showTab(TAB_SETTINGS));
        showTab(savedInstanceState != null ? savedInstanceState.getInt(STATE_TAB, TAB_HEUTE) : TAB_HEUTE);

        // ---- Heute ----
        btnStart.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            SessionManager.start(this, now, "manuell");
            Work.DayInfo di = SessionManager.dayInfo(this, now);
            if (di.earlierSessions > 0 && di.remainingNet <= 0) {
                toast("Achtung: Die " + Fmt.hoursFromMinutes(Prefs.maxWorkMin(this))
                        + " h sind heute schon erreicht!");
            } else {
                toast("Eingestempelt um " + Fmt.time(now) + " · spätestens raus: " + Fmt.timeSmart(di.deadline));
            }
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

        handleIntent(getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /** Zurück-Taste schließt zuerst das Einstellungsmenü. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (currentTab == TAB_SETTINGS) {
            showTab(lastMainTab);
        } else {
            super.onBackPressed();
        }
    }

    /** Aus der Benachrichtigung "Startzeit anpassen" geöffnet? Dann gleich die Uhrzeitauswahl zeigen. */
    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_EDIT_START, false)) {
            intent.removeExtra(EXTRA_EDIT_START);
            showTab(TAB_HEUTE);
            handler.post(this::editStart);
        }
    }

    // =====================================================================
    // Reiter
    // =====================================================================

    /** 0 = Heute, 1 = Arbeitszeit, 2 = Einstellungen. */
    void showTab(int tab) {
        currentTab = tab;
        if (tab != TAB_SETTINGS) lastMainTab = tab;
        boolean inSettings = tab == TAB_SETTINGS;

        OvertimeTab.show(scrollHeute, tab == TAB_HEUTE);
        OvertimeTab.show(scrollZeit, tab == TAB_ZEIT);
        OvertimeTab.show(scrollSettings, inSettings);
        OvertimeTab.show(tabBar, !inSettings);

        tvTitle.setText(inSettings ? "Einstellungen" : getString(R.string.app_name));
        btnSettings.setText(inSettings ? "✕" : "⚙");
        styleTab(tabHeute, tab == TAB_HEUTE);
        styleTab(tabZeit, tab == TAB_ZEIT);

        if (tab == TAB_ZEIT) overtime.refresh();
        if (inSettings) settings.refresh();
    }

    private void styleTab(TextView t, boolean selected) {
        if (selected) {
            t.setBackgroundResource(R.drawable.tab_selected);
            t.setTextColor(getColor(R.color.on_primary));
        } else {
            t.setBackground(null);
            t.setTextColor(getColor(R.color.text_secondary));
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        settings.onPermissionResult(requestCode);
        refreshAll();
    }

    // =====================================================================
    // Anzeige
    // =====================================================================

    void refreshAll() {
        refreshStatus();
        OvertimeTab.show(tvSetupHint, !settings.setupComplete());
        if (currentTab == TAB_ZEIT) overtime.refresh();
        if (currentTab == TAB_SETTINGS) settings.refresh();
    }

    /** Nochmal aktualisieren, sobald Android die Ortserkennung eingerichtet hat. */
    void postRefresh() {
        handler.postDelayed(this::refreshAll, 1500);
    }

    private void refreshStatus() {
        boolean active = Prefs.active(this);
        long now = System.currentTimeMillis();

        if (active) {
            long start = Prefs.start(this);
            Work.DayInfo di = SessionManager.dayInfo(this, start);
            long dl = di.deadline;
            long left = dl - now;

            tvStatus.setText((di.earlierSessions > 0 ? (di.earlierSessions + 1) + ". Einsatz heute – e" : "E")
                    + "ingestempelt seit " + Fmt.timeSmart(start) + "  (" + Prefs.source(this) + ")");
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
            String worked = "Anwesend: " + Fmt.duration(gross)
                    + "  ·  netto ca. " + Fmt.duration(Work.netForGross(gross, Prefs.rules(this)));
            if (di.earlierSessions > 0) {
                worked += "\nFrühere Einsätze heute: " + Fmt.duration(di.earlierNet) + " netto"
                        + "\nGrenze = Start + noch " + Fmt.duration(di.remainingNet) + " Arbeit"
                        + (di.extraBreak > 0 ? " + " + (di.extraBreak / Work.MIN) + " Min. Pause" : "")
                        + " (Lücke zählt als Pause)";
            } else {
                worked += "\nGrenze = Start + " + Fmt.hoursFromMinutes(Prefs.maxWorkMin(this)) + " h + "
                        + Prefs.breakMin(this) + " Min. Pause";
            }
            tvWorked.setText(worked);
        } else {
            Work.DayInfo di = SessionManager.dayInfo(this, now);
            if (di.earlierSessions > 0) {
                tvStatus.setText("Ausgestempelt. Heute schon " + Fmt.duration(di.earlierNet) + " netto gearbeitet"
                        + (di.earlierSessions > 1 ? " (" + di.earlierSessions + " Einsätze)." : "."));
                tvDeadline.setText(di.remainingNet > 0
                        ? "Noch möglich: " + Fmt.durationWords(di.remainingNet)
                        : Fmt.hoursFromMinutes(Prefs.maxWorkMin(this)) + " h erreicht");
                tvRemaining.setText("Nochmal arbeiten? „Erneut einstempeln“ tippen – die Grenze wird automatisch angepasst.");
                tvRemaining.setTextColor(getColor(di.remainingNet > 0 ? R.color.text_secondary : R.color.danger));
            } else {
                tvStatus.setText("Nicht eingestempelt.");
                tvDeadline.setText("—");
                tvRemaining.setText(Prefs.trackingEnabled(this)
                        ? "Wartet auf Ankunft am Arbeitsort …"
                        : "Automatische Erkennung ist aus.");
                tvRemaining.setTextColor(getColor(R.color.text_secondary));
            }
            tvWorked.setText("");
            btnStart.setText(di.earlierSessions > 0 ? "Erneut einstempeln" : "Jetzt eingestempelt");
        }

        btnStart.setEnabled(!active);
        btnEnd.setEnabled(active);
        btnDiscard.setEnabled(active);
        btnMinus.setEnabled(active);
        btnPlus.setEnabled(active);
        btnEditStart.setText(active ? "Start " + Fmt.time(Prefs.start(this)) + " ändern" : "Startzeit festlegen");
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

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
