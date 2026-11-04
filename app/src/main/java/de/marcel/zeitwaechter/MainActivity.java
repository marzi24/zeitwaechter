package de.marcel.zeitwaechter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Der Bildschirm der App mit der Navigation unten:
 * „Heute“, „Arbeitszeit“ und „Einstellungen“.
 */
public class MainActivity extends Activity {

    /** Wenn gesetzt, öffnet die App direkt die Uhrzeitauswahl für die Startzeit. */
    static final String EXTRA_EDIT_START = "edit_start";

    private static final String STATE_TAB = "tab";
    static final int TAB_HEUTE = 0;
    static final int TAB_ZEIT = 1;
    static final int TAB_SETTINGS = 2;

    private static final DateTimeFormatter DATE_LONG =
            DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMANY);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshAll();
            handler.postDelayed(this, 15_000);
        }
    };

    // Heute
    private TextView tvDate, tvStatusChip, tvRingSmall, tvRingBig, tvRingUnit, tvPlusChip;
    private TextView tvDeadlineLabel, tvDeadline, tvDeadlineNote, tvStatGross, tvStatNet, tvStatDiff;
    private TextView tvStartTime, tvStartLabel, tvAlertTitle, tvAlertText, tvWeekChip;
    private TextView btnEditStart, btnMinus, btnPlus;
    private View setupHint, alertCard, activeBox;
    private Button btnStart, btnEnd, btnDiscard;
    private RingView ring;
    private WeekBarsView weekBars;

    // Navigation
    private View scrollHeute, scrollZeit, scrollSettings, scrollDetail, fab;
    private OvertimeTab overtime;
    private SettingsTab settings;
    private int currentTab = TAB_HEUTE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notifier.createChannels(this);

        tvDate = findViewById(R.id.tvDate);
        tvStatusChip = findViewById(R.id.tvStatusChip);
        tvRingSmall = findViewById(R.id.tvRingSmall);
        tvRingBig = findViewById(R.id.tvRingBig);
        tvRingUnit = findViewById(R.id.tvRingUnit);
        tvPlusChip = findViewById(R.id.tvPlusChip);
        tvDeadlineLabel = findViewById(R.id.tvDeadlineLabel);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvDeadlineNote = findViewById(R.id.tvDeadlineNote);
        tvStatGross = findViewById(R.id.tvStatGross);
        tvStatNet = findViewById(R.id.tvStatNet);
        tvStatDiff = findViewById(R.id.tvStatDiff);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvStartLabel = findViewById(R.id.tvStartLabel);
        tvAlertTitle = findViewById(R.id.tvAlertTitle);
        tvAlertText = findViewById(R.id.tvAlertText);
        tvWeekChip = findViewById(R.id.tvWeekChip);
        btnEditStart = findViewById(R.id.btnEditStart);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        setupHint = findViewById(R.id.tvSetupHint);
        alertCard = findViewById(R.id.alertCard);
        activeBox = findViewById(R.id.activeBox);
        btnStart = findViewById(R.id.btnStart);
        btnEnd = findViewById(R.id.btnEnd);
        btnDiscard = findViewById(R.id.btnDiscard);
        ring = findViewById(R.id.ringView);
        weekBars = findViewById(R.id.weekBars);

        scrollHeute = findViewById(R.id.scrollHeute);
        scrollZeit = findViewById(R.id.scrollZeit);
        scrollSettings = findViewById(R.id.scrollSettings);
        scrollDetail = findViewById(R.id.scrollDetail);
        fab = findViewById(R.id.btnFab);

        overtime = new OvertimeTab(this);
        settings = new SettingsTab(this);

        // „+ Eintrag“ beim Runterscrollen ausblenden, damit er keine Zahlen verdeckt
        scrollZeit.setOnScrollChangeListener((v, x, y, ox, oy) -> {
            if (currentTab != TAB_ZEIT) return;
            if (y > oy + 4) fab.animate().translationY(fab.getHeight() * 2f).setDuration(150).start();
            else if (y < oy - 4 || y == 0) fab.animate().translationY(0).setDuration(150).start();
        });

        findViewById(R.id.navHeute).setOnClickListener(v -> showTab(TAB_HEUTE));
        findViewById(R.id.navZeit).setOnClickListener(v -> showTab(TAB_ZEIT));
        findViewById(R.id.navSettings).setOnClickListener(v -> showTab(TAB_SETTINGS));
        setupHint.setOnClickListener(v -> {
            showTab(TAB_SETTINGS);
            settings.openSetup();
        });

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

        showTab(savedInstanceState != null ? savedInstanceState.getInt(STATE_TAB, TAB_HEUTE) : TAB_HEUTE);
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

    /** Zurück-Taste: Detailseite → Einstellungsliste → Heute → App schließen. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (settings.isDetailOpen()) {
            settings.closeDetail();
        } else if (currentTab != TAB_HEUTE) {
            showTab(TAB_HEUTE);
        } else {
            super.onBackPressed();
        }
    }

    /** Aus der Benachrichtigung "Startzeit anpassen" geöffnet? Dann gleich die Uhrzeitauswahl zeigen. */
    private void handleIntent(Intent intent) {
        // Direkt einen Bereich öffnen (z. B. für automatische Screenshots): --ei tab 0/1/2
        if (intent != null && intent.hasExtra("tab")) {
            showTab(intent.getIntExtra("tab", TAB_HEUTE));
            intent.removeExtra("tab");
        }
        if (intent != null && intent.getBooleanExtra(EXTRA_EDIT_START, false)) {
            intent.removeExtra(EXTRA_EDIT_START);
            showTab(TAB_HEUTE);
            handler.post(this::editStart);
        }
    }

    // =====================================================================
    // Navigation unten
    // =====================================================================

    /** 0 = Heute, 1 = Arbeitszeit, 2 = Einstellungen. */
    void showTab(int tab) {
        currentTab = tab;
        if (settings.isDetailOpen() && tab != TAB_SETTINGS) settings.closeDetail();
        OvertimeTab.show(scrollHeute, tab == TAB_HEUTE);
        OvertimeTab.show(scrollZeit, tab == TAB_ZEIT);
        OvertimeTab.show(fab, tab == TAB_ZEIT);
        OvertimeTab.show(scrollSettings, tab == TAB_SETTINGS && !settings.isDetailOpen());
        OvertimeTab.show(scrollDetail, tab == TAB_SETTINGS && settings.isDetailOpen());

        styleNav(R.id.navHeutePill, R.id.navHeuteIcon, R.id.navHeuteLabel, tab == TAB_HEUTE);
        styleNav(R.id.navZeitPill, R.id.navZeitIcon, R.id.navZeitLabel, tab == TAB_ZEIT);
        styleNav(R.id.navSettingsPill, R.id.navSettingsIcon, R.id.navSettingsLabel, tab == TAB_SETTINGS);

        if (tab == TAB_HEUTE) refreshStatus();
        if (tab == TAB_ZEIT) overtime.refresh();
        if (tab == TAB_SETTINGS) settings.refresh();
    }

    private void styleNav(int pillId, int iconId, int labelId, boolean on) {
        View pill = findViewById(pillId);
        ImageView icon = findViewById(iconId);
        TextView label = findViewById(labelId);
        if (on) {
            pill.setBackgroundResource(R.drawable.bg_nav_pill);
        } else {
            pill.setBackground(null);
        }
        icon.setImageTintList(ColorStateList.valueOf(getColor(on ? R.color.primary : R.color.text_secondary)));
        label.setTextColor(getColor(on ? R.color.text : R.color.text_secondary));
    }

    /** Zeigt Liste oder Detailseite der Einstellungen (wird von SettingsTab aufgerufen). */
    void showSettingsPage(boolean detail) {
        OvertimeTab.show(scrollSettings, !detail);
        OvertimeTab.show(scrollDetail, detail);
        scrollDetail.scrollTo(0, 0);
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
        if (currentTab == TAB_ZEIT) overtime.refresh();
        if (currentTab == TAB_SETTINGS) settings.refresh();
    }

    /** Nochmal aktualisieren, sobald Android die Ortserkennung eingerichtet hat. */
    void postRefresh() {
        handler.postDelayed(this::refreshAll, 1500);
    }

    private void refreshStatus() {
        long now = System.currentTimeMillis();
        boolean active = Prefs.active(this);
        long start = active ? Prefs.start(this) : 0;
        Work.Rules rules = Prefs.rules(this);
        List<Prefs.Entry> hist = Prefs.history(this);
        Work.Today t = Work.today(hist, active, start, now, Prefs.maxWorkMin(this), rules);
        Work.DayInfo di = t.di;

        tvDate.setText(capitalize(DATE_LONG.format(LocalDate.now())));
        OvertimeTab.show(setupHint, !settings.setupComplete());

        int ok = getColor(R.color.ok);
        int danger = getColor(R.color.danger);
        int ringColor = t.plus() ? ok : danger;

        if (active) {
            // ---- Plakette oben ----
            if (di.earlierSessions > 0) {
                chip(tvStatusChip, (di.earlierSessions + 1) + ". Einsatz heute", R.drawable.bg_chip_warn, R.color.warn);
            } else {
                chip(tvStatusChip, "Eingestempelt · " + Prefs.source(this), R.drawable.bg_chip_ok, R.color.ok);
            }

            // ---- Ring ----
            ring.set(t.progress, ringColor, t.marker);
            long left = t.left;
            if (left > 0) {
                tvRingSmall.setText("noch");
                setBig(left);
            } else {
                tvRingSmall.setText("Grenze überschritten");
                setBig(-left);
            }
            tvRingBig.setTextColor(getColor(left > 0 ? R.color.text : R.color.danger));

            // ---- Plus/Minus ----
            OvertimeTab.show(tvPlusChip, true);
            if (t.plus()) {
                chip(tvPlusChip, Fmt.signed(t.diff) + " h im Plus", R.drawable.bg_chip_ok, R.color.ok);
            } else if (t.plusAt > 0) {
                chip(tvPlusChip, Fmt.signed(t.diff) + " h · Plus ab " + Fmt.time(t.plusAt) + " Uhr",
                        R.drawable.bg_chip_danger, R.color.danger);
            } else {
                chip(tvPlusChip, Fmt.signed(t.diff) + " h · Plus vor der Grenze nicht mehr möglich",
                        R.drawable.bg_chip_danger, R.color.danger);
            }

            // ---- Grenze ----
            tvDeadlineLabel.setText("Spätestens raus");
            OvertimeTab.show(tvDeadline, true);
            tvDeadline.setText(Fmt.time(t.deadline) + " Uhr");
            boolean otherDay = !Work.day(t.deadline).equals(LocalDate.now());
            String dayPrefix = otherDay ? Fmt.day(t.deadline) + " · " : "";
            List<Integer> warn = Prefs.warnMinutes(this);
            int firstWarn = 0;
            for (int m : warn) firstWarn = Math.max(firstWarn, m);
            if (di.byClock) {
                tvDeadlineNote.setText(dayPrefix + "Späteste Ausstempelzeit · 10-h-Grenze wäre " + Fmt.timeSmart(di.workDeadline));
            } else if (di.earlierSessions > 0) {
                tvDeadlineNote.setText(dayPrefix + "Vorher in dieser Schicht " + Fmt.duration(di.earlierNet) + " netto · Lücke zählt als Pause");
            } else {
                tvDeadlineNote.setText(dayPrefix + Fmt.hoursFromMinutes(Prefs.maxWorkMin(this)) + " h + "
                        + rules.pauseMin + " Min. Pause"
                        + (firstWarn > 0 ? " · Warnung " + firstWarn + " Min. vorher" : ""));
            }

            // ---- Warnbanner ----
            boolean alarm = left <= 15 * Work.MIN;
            OvertimeTab.show(alertCard, alarm);
            if (alarm) {
                tvAlertTitle.setText(left > 0 ? "Gleich ausstempeln!" : "Grenze überschritten!");
                String limit = Fmt.hoursFromMinutes(Prefs.maxWorkMin(this)) + "-h-Grenze";
                if (di.byClock) {
                    tvAlertText.setText(left > 0
                            ? "Um " + Fmt.time(di.clockDeadline) + " Uhr ist Schluss – spätere Zeiten sind nicht erlaubt."
                            : "Seit " + Fmt.time(di.clockDeadline) + " Uhr ist Schluss – bitte sofort ausstempeln.");
                } else {
                    tvAlertText.setText(left > 0
                            ? "Um " + Fmt.time(t.deadline) + " Uhr erreichst du die " + limit + "."
                            : "Die " + limit + " wurde um " + Fmt.time(t.deadline) + " Uhr erreicht – bitte sofort ausstempeln.");
                }
            }
            btnEnd.setBackgroundResource(alarm ? R.drawable.bg_btn_danger : R.drawable.bg_btn_filled);

            // ---- Kacheln ----
            tvStatGross.setText(Fmt.hm(t.sessionGross));
            tvStatNet.setText(Fmt.hm(t.dayNet));
            tvStatDiff.setText(Fmt.signed(t.diff));
            tvStatDiff.setTextColor(ringColor);

            // ---- Stempel-Karte ----
            OvertimeTab.show(activeBox, true);
            OvertimeTab.show(btnStart, false);
            tvStartTime.setText(Fmt.time(start) + " Uhr");
            tvStartLabel.setText(Work.day(start).equals(LocalDate.now())
                    ? "Eingestempelt" : "Eingestempelt am " + Fmt.day(start));
        } else {
            OvertimeTab.show(alertCard, false);
            OvertimeTab.show(activeBox, false);
            OvertimeTab.show(btnStart, true);
            ring.set(t.progress, ringColor, t.marker);
            tvRingBig.setTextColor(getColor(R.color.text));

            if (t.hasWork) {
                chip(tvStatusChip, "Ausgestempelt", R.drawable.bg_chip_primary, R.color.primary);
                long possible = Math.max(0, Math.min(di.remainingNet, di.deadline - now - di.extraBreak));
                tvRingSmall.setText("heute netto");
                tvRingBig.setText(Fmt.hm(t.dayNet));
                tvRingUnit.setText("Stunden");
                OvertimeTab.show(tvPlusChip, true);
                chip(tvPlusChip, Fmt.signed(t.diff) + (t.plus() ? " h im Plus" : " h im Minus"),
                        t.plus() ? R.drawable.bg_chip_ok : R.drawable.bg_chip_danger, t.plus() ? R.color.ok : R.color.danger);
                tvDeadlineLabel.setText(possible > 0 ? "Noch möglich" : "Heute");
                OvertimeTab.show(tvDeadline, true);
                tvDeadline.setText(possible > 0 ? Fmt.durationWords(possible)
                        : (di.byClock ? Fmt.time(di.clockDeadline) + " Uhr erreicht"
                        : Fmt.hoursFromMinutes(Prefs.maxWorkMin(this)) + " h erreicht"));
                tvDeadlineNote.setText("Nochmal arbeiten? „Erneut einstempeln“ – die Grenze wird angepasst.");
                btnStart.setText("Erneut einstempeln");
                tvStatDiff.setText(Fmt.signed(t.diff));
                tvStatDiff.setTextColor(ringColor);
            } else {
                boolean tracking = Prefs.trackingEnabled(this);
                chip(tvStatusChip, tracking ? "Nicht eingestempelt" : "Automatische Erkennung aus",
                        R.drawable.bg_chip_primary, R.color.primary);
                tvRingSmall.setText("heute");
                tvRingBig.setText("0:00");
                tvRingUnit.setText("gearbeitet");
                OvertimeTab.show(tvPlusChip, false);
                tvDeadlineLabel.setText(tracking ? "Wartet auf Ankunft am Arbeitsort" : "Einstempeln von Hand");
                OvertimeTab.show(tvDeadline, false);
                tvDeadlineNote.setText(t.soll > 0
                        ? "Soll heute " + Fmt.hm(t.soll) + " h · ab dann zählt jede Minute als Plus"
                        : "Heute keine Sollzeit – jede Stunde zählt als Überstunde");
                btnStart.setText("Jetzt einstempeln");
                tvStatDiff.setText("—");
                tvStatDiff.setTextColor(getColor(R.color.text));
            }
            tvStatGross.setText("—");
            tvStatNet.setText(Fmt.hm(t.dayNet));
        }

        refreshWeek(hist, active, start, now, rules);
    }

    /** Große Zahl im Ring: ab 1 h als „7:34 Stunden“, darunter als „14 Minuten“. */
    private void setBig(long ms) {
        long min = ms / Work.MIN;
        if (min >= 60) {
            tvRingBig.setText(Fmt.hm(ms));
            tvRingUnit.setText("Stunden");
        } else {
            tvRingBig.setText(String.valueOf(min));
            tvRingUnit.setText(min == 1 ? "Minute" : "Minuten");
        }
    }

    private void refreshWeek(List<Prefs.Entry> hist, boolean active, long start, long now, Work.Rules rules) {
        long[] net = Work.weekNet(hist, active, start, now, rules);
        LocalDate today = Work.day(now);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        boolean[] has = new boolean[7];
        for (Prefs.Entry e : hist) {
            long i = Work.day(e.start).toEpochDay() - monday.toEpochDay();
            if (i >= 0 && i < 7) has[(int) i] = true;
        }
        if (active) {
            long i = Work.day(start).toEpochDay() - monday.toEpochDay();
            if (i >= 0 && i < 7) has[(int) i] = true;
        }
        long diff = Work.weekDiff(net, monday, has, rules);
        boolean any = false;
        for (boolean b : has) any |= b;
        if (any) {
            chip(tvWeekChip, Fmt.signed(diff) + " h", diff >= 0 ? R.drawable.bg_chip_ok : R.drawable.bg_chip_danger,
                    diff >= 0 ? R.color.ok : R.color.danger);
        } else {
            chip(tvWeekChip, "noch nichts", R.drawable.bg_chip_primary, R.color.primary);
        }
        weekBars.set(net, today.getDayOfWeek().getValue() - 1, rules.sollMin * Work.MIN);
    }

    private void chip(TextView tv, String text, int bg, int color) {
        tv.setText(text);
        tv.setBackgroundResource(bg);
        tv.setTextColor(getColor(color));
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
