package de.marcel.zeitwaechter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Einstellungen: eine Liste mit Zusammenfassungen. Ein Tipp auf eine Zeile öffnet
 * die passende Detailseite (Einrichtung, Arbeitsort, Erkennung, Grenzen, Soll & Pause,
 * Überstunden, Zuschläge, Feiertage).
 */
final class SettingsTab {

    static final int REQ_BASIC = 1;
    static final int REQ_BACKGROUND = 2;

    private final MainActivity a;

    private final TextView tvPerms, tvGeofence, tvHolidayList, tvVersion, tvPauseExample;
    private final EditText etLat, etLon, etRadius, etOffset, etOffsetOut, etMaxHours, etLatestOut, etBreak, etWarn, etResume;
    private final EditText etSoll, etPauseAfter, etStartBal, etLimit, etRate;
    private final EditText etSurWeek, etSurSatLow, etSatLowHours, etSurSatHigh, etSurSun, etSurHol, etExtraHol;
    private final Switch swEnabled;
    private final Spinner spState;
    private final Button btnStartDate, btnSaveAll;
    private final Switch swAutoList;
    private final TextView tvDetailTitle, tvSetupTitle, tvSetupSub;
    private final TextView tvRowHome, tvHomeStatus;
    private final EditText etHomeLat, etHomeLon, etHomeRadius;
    private final TextView tvRowOrt, tvRowAuto, tvRowWege, tvRowGrenzen, tvRowWarn, tvRowSoll, tvRowOt,
            tvRowZuschlag, tvRowFeiertage;
    private final android.widget.ImageView setupIcon;
    private final View setupIconBg;
    private final View[] sections;
    private LocalDate startDate;
    private View openSection;

    SettingsTab(MainActivity activity) {
        a = activity;
        tvPerms = a.findViewById(R.id.tvPerms);
        tvGeofence = a.findViewById(R.id.tvGeofence);
        tvHolidayList = a.findViewById(R.id.tvHolidayList);
        tvVersion = a.findViewById(R.id.tvVersion);
        tvPauseExample = a.findViewById(R.id.tvPauseExample);

        etLat = a.findViewById(R.id.etLat);
        etLon = a.findViewById(R.id.etLon);
        etRadius = a.findViewById(R.id.etRadius);
        etOffset = a.findViewById(R.id.etOffset);
        etOffsetOut = a.findViewById(R.id.etOffsetOut);
        etMaxHours = a.findViewById(R.id.etMaxHours);
        etLatestOut = a.findViewById(R.id.etLatestOut);
        etBreak = a.findViewById(R.id.etBreak);
        etWarn = a.findViewById(R.id.etWarn);
        etResume = a.findViewById(R.id.etResume);
        swEnabled = a.findViewById(R.id.swEnabled);

        etSoll = a.findViewById(R.id.etSoll);
        etPauseAfter = a.findViewById(R.id.etPauseAfter);
        etStartBal = a.findViewById(R.id.etStartBal);
        etLimit = a.findViewById(R.id.etLimit);
        etRate = a.findViewById(R.id.etRate);
        btnStartDate = a.findViewById(R.id.btnStartDate);

        etSurWeek = a.findViewById(R.id.etSurWeek);
        etSurSatLow = a.findViewById(R.id.etSurSatLow);
        etSatLowHours = a.findViewById(R.id.etSatLowHours);
        etSurSatHigh = a.findViewById(R.id.etSurSatHigh);
        etSurSun = a.findViewById(R.id.etSurSun);
        etSurHol = a.findViewById(R.id.etSurHol);
        etExtraHol = a.findViewById(R.id.etExtraHol);
        spState = a.findViewById(R.id.spState);
        btnSaveAll = a.findViewById(R.id.btnSaveAll);
        swAutoList = a.findViewById(R.id.swAutoList);
        tvDetailTitle = a.findViewById(R.id.tvDetailTitle);
        tvSetupTitle = a.findViewById(R.id.tvSetupTitle);
        tvSetupSub = a.findViewById(R.id.tvSetupSub);
        setupIcon = a.findViewById(R.id.setupIcon);
        setupIconBg = a.findViewById(R.id.setupIconBg);
        tvRowOrt = a.findViewById(R.id.tvRowOrt);
        tvRowHome = a.findViewById(R.id.tvRowHome);
        tvHomeStatus = a.findViewById(R.id.tvHomeStatus);
        etHomeLat = a.findViewById(R.id.etHomeLat);
        etHomeLon = a.findViewById(R.id.etHomeLon);
        etHomeRadius = a.findViewById(R.id.etHomeRadius);
        tvRowAuto = a.findViewById(R.id.tvRowAuto);
        tvRowWege = a.findViewById(R.id.tvRowWege);
        tvRowGrenzen = a.findViewById(R.id.tvRowGrenzen);
        tvRowWarn = a.findViewById(R.id.tvRowWarn);
        tvRowSoll = a.findViewById(R.id.tvRowSoll);
        tvRowOt = a.findViewById(R.id.tvRowOt);
        tvRowZuschlag = a.findViewById(R.id.tvRowZuschlag);
        tvRowFeiertage = a.findViewById(R.id.tvRowFeiertage);
        sections = new View[]{
                a.findViewById(R.id.secSetup), a.findViewById(R.id.secOrt), a.findViewById(R.id.secHome),
                a.findViewById(R.id.secErkennung),
                a.findViewById(R.id.secGrenzen), a.findViewById(R.id.secSoll), a.findViewById(R.id.secOt),
                a.findViewById(R.id.secZuschlag), a.findViewById(R.id.secFeiertage)};

        // ---- Liste: Zeilen öffnen Detailseiten ----
        a.findViewById(R.id.rowSetup).setOnClickListener(v -> openSetup());
        a.findViewById(R.id.rowOrt).setOnClickListener(v -> openDetail(R.id.secOrt, "Arbeitsort"));
        a.findViewById(R.id.rowHome).setOnClickListener(v -> openDetail(R.id.secHome, "Zuhause (Homeoffice)"));
        a.findViewById(R.id.btnHomeUseCurrent).setOnClickListener(v -> homeFromCurrentLocation());
        a.findViewById(R.id.btnHomePaste).setOnClickListener(v -> homeFromClipboard());
        a.findViewById(R.id.btnHomeSave).setOnClickListener(v -> homeSaveFields());
        a.findViewById(R.id.btnHomeClear).setOnClickListener(v -> new AlertDialog.Builder(a)
                .setTitle("Zuhause entfernen?")
                .setMessage("Einsätze werden dann nicht mehr automatisch als Homeoffice erkannt.")
                .setPositiveButton("Entfernen", (d, w) -> {
                    Prefs.clearHomeLocation(a);
                    fillHomeFields();
                    refresh();
                })
                .setNegativeButton("Abbrechen", null)
                .show());
        a.findViewById(R.id.rowWege).setOnClickListener(v -> openDetail(R.id.secErkennung, "Erkennung & Wege"));
        a.findViewById(R.id.rowGrenzen).setOnClickListener(v -> openDetail(R.id.secGrenzen, "Grenzen & Warnungen"));
        a.findViewById(R.id.rowWarn).setOnClickListener(v -> openDetail(R.id.secGrenzen, "Grenzen & Warnungen"));
        a.findViewById(R.id.rowSoll).setOnClickListener(v -> openDetail(R.id.secSoll, "Soll & Pause"));
        a.findViewById(R.id.rowOt).setOnClickListener(v -> openDetail(R.id.secOt, "Überstunden"));
        a.findViewById(R.id.rowZuschlag).setOnClickListener(v -> openDetail(R.id.secZuschlag, "Zuschläge"));
        a.findViewById(R.id.rowFeiertage).setOnClickListener(v -> openDetail(R.id.secFeiertage, "Feiertage"));
        a.findViewById(R.id.rowAuto).setOnClickListener(v -> swAutoList.toggle());
        a.findViewById(R.id.btnBack).setOnClickListener(v -> closeDetail());
        swAutoList.setOnCheckedChangeListener((b, on) -> {
            if (on == Prefs.trackingEnabled(a)) return;
            Prefs.setTrackingEnabled(a, on);
            swEnabled.setChecked(on);
            GeofenceHelper.register(a);
            refreshList();
            a.refreshAll();
            a.postRefresh();
        });

        // ---- Einrichtung ----
        a.findViewById(R.id.btnPermLocation).setOnClickListener(v -> requestBasicPermissions());
        a.findViewById(R.id.btnPermBackground).setOnClickListener(v -> requestBackgroundLocation());
        a.findViewById(R.id.btnPermBattery).setOnClickListener(v -> requestBatteryAndAlarms());

        // ---- Arbeitsort ----
        a.findViewById(R.id.btnUseCurrent).setOnClickListener(v -> new AlertDialog.Builder(a)
                .setTitle("Aktuellen Standort speichern?")
                .setMessage("Mach das am besten, wenn du auf dem Parkplatz bei der Arbeit stehst. "
                        + "Der Kreis (Radius) wird um diesen Punkt gelegt.")
                .setPositiveButton("Speichern", (d, w) -> useCurrentLocation())
                .setNegativeButton("Abbrechen", null)
                .show());
        a.findViewById(R.id.btnOpenMap).setOnClickListener(v -> openMap());
        a.findViewById(R.id.btnSaveLocation).setOnClickListener(v -> saveLocationOnly());
        a.findViewById(R.id.btnPasteLocation).setOnClickListener(v -> pasteLocation());
        etLon.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveLocationOnly();
                return true;
            }
            return false;
        });

        // ---- Überstunden ----
        btnStartDate.setOnClickListener(v -> new DatePickerDialog(a, (view, y, m, d) -> {
            startDate = LocalDate.of(y, m + 1, d);
            updateStartDateButton();
        }, startDate.getYear(), startDate.getMonthValue() - 1, startDate.getDayOfMonth()).show());

        // ---- Feiertage ----
        ArrayAdapter<String> adapter = new ArrayAdapter<>(a, android.R.layout.simple_spinner_item,
                Holidays.STATE_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spState.setAdapter(adapter);
        spState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showHolidayList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nichts
            }
        });

        btnSaveAll.setOnClickListener(v -> saveAll());

        fillFields();
        showVersion();
    }

    // =====================================================================
    // Anzeige
    // =====================================================================

    void refresh() {
        refreshList();
        tvPerms.setText(mark(GeofenceHelper.hasFine(a)) + "  Standort (genau)\n"
                + mark(GeofenceHelper.hasBackground(a)) + "  Standort „Immer zulassen“\n"
                + mark(notificationsAllowed()) + "  Benachrichtigungen\n"
                + mark(batteryOk()) + "  Akku-Optimierung aus\n"
                + mark(SessionManager.canExact(a)) + "  Pünktliche Wecker\n"
                + mark(Prefs.hasWorkLocation(a)) + "  Arbeitsort gespeichert");
        tvGeofence.setText("Ortserkennung: " + Prefs.geofenceStatus(a));
    }

    // =====================================================================
    // Liste und Detailseiten
    // =====================================================================

    boolean isDetailOpen() {
        return openSection != null;
    }

    void openSetup() {
        openDetail(R.id.secSetup, "Einrichtung");
    }

    private void openDetail(int sectionId, String title) {
        fillFields(); // immer den gespeicherten Stand zeigen
        for (View v : sections) OvertimeTab.show(v, v.getId() == sectionId);
        openSection = a.findViewById(sectionId);
        tvDetailTitle.setText(title);
        // Einrichtung und Arbeitsort haben eigene Knöpfe
        OvertimeTab.show(btnSaveAll, sectionId != R.id.secSetup && sectionId != R.id.secOrt
                && sectionId != R.id.secHome);
        refresh();
        a.showSettingsPage(true);
    }

    void closeDetail() {
        openSection = null;
        hideKeyboard();
        refreshList();
        a.showSettingsPage(false);
    }

    /** Zusammenfassungen in der Liste aktualisieren. */
    private void refreshList() {
        boolean ok = setupComplete();
        tvSetupTitle.setText(ok ? "Alles eingerichtet" : "Einrichtung unvollständig");
        tvSetupSub.setText(ok ? "Standort, Benachrichtigungen, Akku, Wecker" : "Hier tippen und die fehlenden Punkte erlauben");
        setupIcon.setImageResource(ok ? R.drawable.ic_check : R.drawable.ic_alert);
        setupIcon.setImageTintList(android.content.res.ColorStateList.valueOf(a.getColor(ok ? R.color.ok : R.color.warn)));
        setupIconBg.setBackgroundResource(ok ? R.drawable.bg_icon_ok : R.drawable.bg_icon_warn);

        tvRowOrt.setText(Prefs.hasWorkLocation(a) ? "Gespeichert · Radius " + Prefs.radius(a) + " m" : "Noch nicht festgelegt");
        tvRowHome.setText(Prefs.hasHomeLocation(a) ? "Gespeichert · Radius " + Prefs.homeRadius(a) + " m"
                : "Noch nicht festgelegt – für die Homeoffice-Erkennung");
        tvHomeStatus.setText(Prefs.hasHomeLocation(a)
                ? String.format(Locale.US, "Gespeichert: %.5f, %.5f · Radius %d m", Prefs.homeLat(a), Prefs.homeLon(a),
                Prefs.homeRadius(a))
                : "Noch kein Zuhause gespeichert. Am einfachsten zu Hause auf „Aktuellen Standort übernehmen“ tippen.");
        boolean auto = Prefs.trackingEnabled(a);
        if (swAutoList.isChecked() != auto) swAutoList.setChecked(auto);
        tvRowAuto.setText(auto ? "Kommen & Gehen per Standort" : "Aus – nur von Hand einstempeln");
        tvRowWege.setText("Morgens +" + Prefs.offsetMin(a) + " Min · abends −" + Prefs.offsetOutMin(a) + " Min");
        int lo = Prefs.latestOutMin(a);
        tvRowGrenzen.setText(Fmt.hoursFromMinutes(Prefs.maxWorkMin(a)) + " h netto"
                + (lo >= 0 ? String.format(Locale.GERMANY, " · spätestens %02d:%02d Uhr", lo / 60, lo % 60) : ""));
        List<Integer> warn = Prefs.warnMinutes(a);
        StringBuilder w = new StringBuilder();
        for (int m : warn) w.append(w.length() > 0 ? " · " : "").append(m);
        tvRowWarn.setText(warn.isEmpty() ? "Nur zur Grenze" : w + " Min. vorher");
        Work.Rules r = Prefs.rules(a);
        tvRowSoll.setText(Fmt.hoursFromMinutes(r.sollMin) + " h Mo–Fr · Pause ab "
                + Fmt.hoursFromMinutes(r.pauseAfterMin) + " h, max. " + r.pauseMin + " Min.");
        tvRowOt.setText("Auszahlung ab " + Fmt.hoursFromMinutes(Prefs.otLimitMin(a)) + " h · "
                + (Prefs.hourlyRate(a) > 0 ? Fmt.euro(Prefs.hourlyRate(a)) + "/h" : "Stundenlohn fehlt"));
        tvRowZuschlag.setText(r.week + " · " + r.satLow + "/" + r.satHigh + " · " + r.sun + " · " + r.hol + " %");
        tvRowFeiertage.setText(Holidays.STATE_NAMES[Holidays.stateIndex(r.state)]
                + (r.extraHolidays.isEmpty() ? "" : " + " + r.extraHolidays));
    }

    // =====================================================================
    // Zuhause (Homeoffice)
    // =====================================================================

    private void fillHomeFields() {
        if (Prefs.hasHomeLocation(a)) {
            etHomeLat.setText(String.format(Locale.US, "%.6f", Prefs.homeLat(a)));
            etHomeLon.setText(String.format(Locale.US, "%.6f", Prefs.homeLon(a)));
        } else {
            etHomeLat.setText("");
            etHomeLon.setText("");
        }
        etHomeRadius.setText(String.valueOf(Prefs.homeRadius(a)));
    }

    private void applyHome(double lat, double lon) {
        Prefs.setHomeLocation(a, lat, lon);
        fillHomeFields();
        hideKeyboard();
        refresh();
        toast(String.format(Locale.US, "Zuhause gespeichert ✔  (%.5f, %.5f)", lat, lon));
    }

    @SuppressLint("MissingPermission")
    private void homeFromCurrentLocation() {
        if (!GeofenceHelper.hasFine(a)) {
            toast("Bitte zuerst unter „Einrichtung“ den Standort erlauben.");
            return;
        }
        toast("Standort wird ermittelt …");
        LocationServices.getFusedLocationProviderClient(a)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(a, loc -> {
                    if (loc == null) {
                        toast("Kein Standort gefunden. Ist GPS an?");
                        return;
                    }
                    applyHome(loc.getLatitude(), loc.getLongitude());
                })
                .addOnFailureListener(a, e -> toast("Fehler: " + e.getMessage()));
    }

    private void homeFromClipboard() {
        ClipboardManager cm = a.getSystemService(ClipboardManager.class);
        String text = null;
        if (cm != null && cm.hasPrimaryClip()) {
            ClipData clip = cm.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence cs = clip.getItemAt(0).coerceToText(a);
                if (cs != null) text = cs.toString();
            }
        }
        double[] c = findCoordinates(text);
        if (c == null) {
            toast("Keine Koordinaten in der Zwischenablage. In Google Maps lange auf dein Zuhause drücken und die Zahlen antippen.");
            return;
        }
        applyHome(c[0], c[1]);
    }

    private void homeSaveFields() {
        try {
            String latS = etHomeLat.getText().toString().trim();
            String lonS = etHomeLon.getText().toString().trim();
            double[] pair = findCoordinates(latS);
            if (pair == null) {
                if (latS.isEmpty() && lonS.isEmpty()) {
                    toast("Bitte Koordinaten eingeben oder einfügen.");
                    return;
                }
                pair = new double[]{parseDouble(latS, "Breitengrad"), parseDouble(lonS, "Längengrad")};
                if (Math.abs(pair[0]) > 90 || Math.abs(pair[1]) > 180) {
                    throw new IllegalArgumentException("Koordinaten ungültig.");
                }
            }
            Prefs.setHomeRadius(a, parseInt(etHomeRadius, "Radius", 50, 2000));
            applyHome(pair[0], pair[1]);
        } catch (IllegalArgumentException e) {
            toast(e.getMessage());
        }
    }

    /** Ist alles Nötige erlaubt und ein Arbeitsort gespeichert? */
    boolean setupComplete() {
        return GeofenceHelper.hasFine(a) && GeofenceHelper.hasBackground(a) && notificationsAllowed()
                && batteryOk() && SessionManager.canExact(a) && Prefs.hasWorkLocation(a);
    }

    private static String mark(boolean ok) {
        return ok ? "✅" : "❌";
    }

    private boolean notificationsAllowed() {
        return a.getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    private boolean batteryOk() {
        PowerManager pm = a.getSystemService(PowerManager.class);
        return pm.isIgnoringBatteryOptimizations(a.getPackageName());
    }

    private void showVersion() {
        try {
            String name = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
            tvVersion.setText("Zeitwächter " + name);
        } catch (Exception e) {
            tvVersion.setText("");
        }
    }

    private void updateStartDateButton() {
        btnStartDate.setText("Überstunden-Stand gilt ab:  " + Fmt.dateOf(startDate) + "  (ändern)");
    }

    /** Zeigt an Beispielen, wie die Pause abgezogen wird. */
    private void showPauseExample(Work.Rules r) {
        long after = r.pauseAfterMin * Work.MIN;
        long full = after + r.pauseMin * Work.MIN;
        long mid = after + Math.min(r.pauseMin, 32) * Work.MIN;
        tvPauseExample.setText("So wird gerechnet:\n"
                + "bis " + Fmt.hm(after) + " h anwesend → keine Pause\n"
                + Fmt.hm(mid) + " h anwesend → " + Fmt.hm(Work.pauseForGross(mid, r)) + " Pause → "
                + Fmt.hm(Work.netForGross(mid, r)) + " h netto\n"
                + "ab " + Fmt.hm(full) + " h anwesend → immer " + r.pauseMin + " Min. Pause\n"
                + "Einzelne Tage kannst du in der Tabelle anders eintragen.");
    }

    private void showHolidayList() {
        int idx = spState.getSelectedItemPosition();
        if (idx < 0) idx = 0;
        String code = Holidays.STATE_CODES[idx];
        int year = LocalDate.now().getYear();
        Map<LocalDate, String> map = Holidays.forYear(year, code, etExtraHol.getText().toString());
        StringBuilder sb = new StringBuilder("Feiertage " + year + " in " + Holidays.STATE_NAMES[idx] + ":");
        for (Map.Entry<LocalDate, String> e : map.entrySet()) {
            sb.append('\n').append(Fmt.dayOf(e.getKey())).append("  ").append(e.getValue());
        }
        tvHolidayList.setText(sb.toString());
    }

    // =====================================================================
    // Felder füllen und speichern
    // =====================================================================

    private void fillFields() {
        swEnabled.setChecked(Prefs.trackingEnabled(a));
        etRadius.setText(String.valueOf(Prefs.radius(a)));
        etOffset.setText(String.valueOf(Prefs.offsetMin(a)));
        etOffsetOut.setText(String.valueOf(Prefs.offsetOutMin(a)));
        etMaxHours.setText(Fmt.hoursFromMinutes(Prefs.maxWorkMin(a)));
        int lo = Prefs.latestOutMin(a);
        etLatestOut.setText(lo >= 0 ? String.format(Locale.GERMANY, "%02d:%02d", lo / 60, lo % 60) : "");
        etBreak.setText(String.valueOf(Prefs.breakMin(a)));
        etWarn.setText(Prefs.warnList(a));
        etResume.setText(String.valueOf(Prefs.resumeMin(a)));
        fillLocationFields();
        fillHomeFields();

        etSoll.setText(Fmt.hoursInput(Prefs.sollMin(a)));
        etPauseAfter.setText(Fmt.hoursInput(Prefs.pauseAfterMin(a)));
        showPauseExample(Prefs.rules(a));
        etStartBal.setText(Fmt.hoursInput(Prefs.startBalanceMin(a)));
        startDate = Prefs.otStartDate(a);
        updateStartDateButton();
        etLimit.setText(Fmt.hoursInput(Prefs.otLimitMin(a)));
        double rate = Prefs.hourlyRate(a);
        etRate.setText(rate > 0 ? String.format(Locale.GERMANY, "%.2f", rate) : "");

        Work.Rules s = Prefs.rules(a);
        etSurWeek.setText(String.valueOf(s.week));
        etSurSatLow.setText(String.valueOf(s.satLow));
        etSatLowHours.setText(Fmt.hoursInput(s.satLowMin));
        etSurSatHigh.setText(String.valueOf(s.satHigh));
        etSurSun.setText(String.valueOf(s.sun));
        etSurHol.setText(String.valueOf(s.hol));
        etExtraHol.setText(s.extraHolidays);
        spState.setSelection(Holidays.stateIndex(s.state));
        showHolidayList();
    }

    /** Knopf „Alle Einstellungen speichern“: erst alles prüfen, dann alles speichern. */
    private void saveAll() {
        try {
            // Erkennung & Erinnerungen
            double[] coords = readCoordFields();
            int radius = parseInt(etRadius, "Radius", 50, 2000);
            int offset = parseInt(etOffset, "Minuten vom Ankommen bis zur Stempeluhr", 0, 30);
            int offsetOut = parseInt(etOffsetOut, "Minuten vom Ausstempeln bis zum Auto", 0, 30);
            int maxWork = hoursToMin(etMaxHours, "Höchstarbeitszeit", 1, 16);
            int latestOut = parseClock(etLatestOut.getText().toString());
            int brk = parseInt(etBreak, "Pause", 0, 180);
            int resume = parseInt(etResume, "Fortsetzen-Zeit", 0, 240);
            String warn = etWarn.getText().toString().trim();
            if (Prefs.parseWarnList(warn).isEmpty() && !warn.isEmpty()) {
                throw new IllegalArgumentException("Warnungen: Zahlen mit Komma, z. B. 30, 15, 5");
            }

            // Überstunden
            int soll = hoursToMin(etSoll, "Sollarbeitszeit", 0.5, 24);
            int pauseAfter = hoursToMin(etPauseAfter, "Pause ab", 0, 24);
            int startBal = hoursToMin(etStartBal, "Überstunden zum Start", -2000, 2000);
            int limit = hoursToMin(etLimit, "Auszahlungsgrenze", 0, 2000);
            String rateTxt = etRate.getText().toString().trim();
            double rate = rateTxt.isEmpty() ? 0 : parseDouble(rateTxt, "Stundenlohn");
            if (rate < 0 || rate > 1000) throw new IllegalArgumentException("Stundenlohn: 0 bis 1000 €.");

            // Zuschläge & Feiertage
            Work.Rules s = new Work.Rules();
            s.week = parseInt(etSurWeek, "Zuschlag Mo–Fr", 0, 500);
            s.satLow = parseInt(etSurSatLow, "Zuschlag Samstag (erste Stunden)", 0, 500);
            s.satLowMin = hoursToMin(etSatLowHours, "Samstag: Anzahl Stunden", 0, 24);
            s.satHigh = parseInt(etSurSatHigh, "Zuschlag Samstag (danach)", 0, 500);
            s.sun = parseInt(etSurSun, "Zuschlag Sonntag", 0, 500);
            s.hol = parseInt(etSurHol, "Zuschlag Feiertag", 0, 500);
            int idx = Math.max(0, spState.getSelectedItemPosition());
            s.state = Holidays.STATE_CODES[idx];
            s.extraHolidays = etExtraHol.getText().toString().trim();
            if (!Holidays.extraValid(s.extraHolidays)) {
                throw new IllegalArgumentException("Zusätzliche Feiertage: Format TT.MM mit Komma, z. B. 24.12, 31.12");
            }

            // Alles gültig -> speichern
            Prefs.saveSettings(a, radius, offset, maxWork, brk, resume, warn, swEnabled.isChecked());
            Prefs.saveOffsetOut(a, offsetOut);
            Prefs.saveLatestOut(a, latestOut);
            if (coords != null) Prefs.setWorkLocation(a, coords[0], coords[1]);
            else Prefs.clearWorkLocation(a);
            Prefs.saveOvertimeSettings(a, soll, pauseAfter, startBal, startDate, limit, rate);
            Prefs.saveRules(a, s);
        } catch (IllegalArgumentException e) {
            toast(e.getMessage());
            return;
        }

        hideKeyboard();
        GeofenceHelper.register(a);
        SessionManager.historyChanged(a); // Höchstarbeitszeit/Pause können die Grenze verändern
        fillFields();
        toast("Gespeichert ✔");
        closeDetail();
        a.refreshAll();
        a.postRefresh();
    }

    // =====================================================================
    // Berechtigungen
    // =====================================================================

    private void requestBasicPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS);
        a.requestPermissions(perms.toArray(new String[0]), REQ_BASIC);
    }

    private void requestBackgroundLocation() {
        if (!GeofenceHelper.hasFine(a)) {
            toast("Bitte zuerst Schritt 1 erledigen und „Genauer Standort“ erlauben.");
            return;
        }
        if (GeofenceHelper.hasBackground(a)) {
            toast("Ist schon erlaubt 👍");
            return;
        }
        new AlertDialog.Builder(a)
                .setTitle("Standort im Hintergrund")
                .setMessage("Damit die App merkt, wann du ankommst, obwohl sie geschlossen ist, "
                        + "wähle im nächsten Fenster bei „Standort“ die Option „Immer zulassen“.")
                .setPositiveButton("Weiter", (d, w) ->
                        a.requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                REQ_BACKGROUND))
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    @SuppressLint("BatteryLife")
    private void requestBatteryAndAlarms() {
        if (!batteryOk()) {
            try {
                a.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + a.getPackageName())));
            } catch (ActivityNotFoundException e) {
                openAppSettings();
            }
            return;
        }
        if (!SessionManager.canExact(a) && Build.VERSION.SDK_INT >= 31) {
            try {
                a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + a.getPackageName())));
            } catch (ActivityNotFoundException e) {
                openAppSettings();
            }
            return;
        }
        toast("Akku & Wecker sind schon richtig eingestellt 👍");
    }

    private void openAppSettings() {
        a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", a.getPackageName(), null)));
    }

    /** Wird von MainActivity.onRequestPermissionsResult aufgerufen. */
    void onPermissionResult(int requestCode) {
        if (requestCode == REQ_BACKGROUND && !GeofenceHelper.hasBackground(a)) {
            toast("Bitte unter „Berechtigungen → Standort“ auf „Immer zulassen“ stellen.");
            openAppSettings();
        }
        if (requestCode == REQ_BASIC && !GeofenceHelper.hasFine(a)) {
            toast("Ohne genauen Standort kann der Arbeitsort nicht erkannt werden.");
        }
        GeofenceHelper.register(a);
    }

    // =====================================================================
    // Arbeitsort
    // =====================================================================

    @SuppressLint("MissingPermission")
    private void useCurrentLocation() {
        if (!GeofenceHelper.hasFine(a)) {
            toast("Bitte zuerst Schritt 1 (Standort erlauben).");
            return;
        }
        toast("Standort wird ermittelt …");
        LocationServices.getFusedLocationProviderClient(a)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(a, loc -> {
                    if (loc == null) {
                        toast("Kein Standort gefunden. Ist GPS an? Am besten draußen nochmal versuchen.");
                        return;
                    }
                    applyLocation(loc.getLatitude(), loc.getLongitude());
                    toast(String.format(Locale.GERMANY,
                            "Arbeitsort gespeichert (Genauigkeit ±%.0f m).", loc.getAccuracy()));
                })
                .addOnFailureListener(a, e -> toast("Fehler: " + e.getMessage()));
    }

    private void openMap() {
        if (!Prefs.hasWorkLocation(a)) {
            toast("Noch kein Arbeitsort gespeichert.");
            return;
        }
        String lat = Double.toString(Prefs.lat(a));
        String lon = Double.toString(Prefs.lon(a));
        Uri uri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(Arbeit)");
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            toast("Keine Karten-App gefunden.");
        }
    }

    private void fillLocationFields() {
        if (Prefs.hasWorkLocation(a)) {
            etLat.setText(String.format(Locale.US, "%.6f", Prefs.lat(a)));
            etLon.setText(String.format(Locale.US, "%.6f", Prefs.lon(a)));
        } else {
            etLat.setText("");
            etLon.setText("");
        }
    }

    /**
     * Liest die beiden Koordinaten-Felder. Erlaubt auch "48.49, 9.21" komplett im ersten Feld.
     * Gibt null zurück, wenn beide Felder leer sind; wirft bei ungültigen Werten.
     */
    private double[] readCoordFields() {
        String latS = etLat.getText().toString().trim();
        String lonS = etLon.getText().toString().trim();
        double[] pair = findCoordinates(latS);
        if (pair != null) return pair;
        if (latS.isEmpty() && lonS.isEmpty()) return null;
        double lat = parseDouble(latS, "Breitengrad");
        double lon = parseDouble(lonS, "Längengrad");
        if (lat < -90 || lat > 90) throw new IllegalArgumentException("Breitengrad muss zwischen -90 und 90 liegen.");
        if (lon < -180 || lon > 180) throw new IllegalArgumentException("Längengrad muss zwischen -180 und 180 liegen.");
        return new double[]{lat, lon};
    }

    /**
     * Sucht Koordinaten in einem Text, z. B.
     * "48.491234, 9.204321" (Google Maps: lange drücken → Koordinaten kopieren) oder
     * "https://www.google.com/maps/place/.../@48.4912,9.2043,17z/data=...!3d48.4913!4d9.2044".
     */
    static double[] findCoordinates(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("!3d(-?\\d{1,2}\\.\\d+)!4d(-?\\d{1,3}\\.\\d+)").matcher(text);
        if (!m.find()) {
            m = Pattern.compile("(-?\\d{1,2}\\.\\d{3,})\\s*[,;]?\\s*(-?\\d{1,3}\\.\\d{3,})").matcher(text);
            if (!m.find()) return null;
        }
        double lat = Double.parseDouble(m.group(1));
        double lon = Double.parseDouble(m.group(2));
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null;
        return new double[]{lat, lon};
    }

    private void saveLocationOnly() {
        double[] c;
        try {
            c = readCoordFields();
        } catch (IllegalArgumentException e) {
            toast(e.getMessage());
            return;
        }
        if (c == null) {
            toast("Bitte Koordinaten eingeben oder einfügen.");
            return;
        }
        applyLocation(c[0], c[1]);
        toast(String.format(Locale.US, "Arbeitsort übernommen ✔  (%.5f, %.5f)", c[0], c[1]));
    }

    private void pasteLocation() {
        ClipboardManager cm = a.getSystemService(ClipboardManager.class);
        String text = null;
        if (cm != null && cm.hasPrimaryClip()) {
            ClipData clip = cm.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence cs = clip.getItemAt(0).coerceToText(a);
                if (cs != null) text = cs.toString();
            }
        }
        if (text == null || text.trim().isEmpty()) {
            toast("Zwischenablage ist leer. In Google Maps lange auf den Ort drücken und die Koordinaten antippen (kopiert sie).");
            return;
        }
        double[] c = findCoordinates(text);
        if (c == null) {
            toast("Keine Koordinaten gefunden. Tipp: In Google Maps lange auf den Ort drücken, oben die Zahlen wie „48.49123, 9.20432“ antippen – dann sind sie kopiert. Kurz-Links (maps.app.goo.gl) enthalten keine Koordinaten.");
            return;
        }
        applyLocation(c[0], c[1]);
        toast(String.format(Locale.US, "Arbeitsort übernommen ✔  (%.5f, %.5f)", c[0], c[1]));
    }

    private void applyLocation(double lat, double lon) {
        Prefs.setWorkLocation(a, lat, lon);
        fillLocationFields();
        hideKeyboard();
        etLat.clearFocus();
        etLon.clearFocus();
        GeofenceHelper.register(a);
        a.refreshAll();
        a.postRefresh();
    }

    // =====================================================================
    // Hilfsfunktionen
    // =====================================================================

    private void hideKeyboard() {
        InputMethodManager imm = a.getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(etLon.getWindowToken(), 0);
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

    /**
     * Uhrzeit wie "20:45", "20.45" oder "2045" in Minuten ab Mitternacht. Leer = -1 (keine Grenze).
     */
    static int parseClock(String text) {
        String t = text.trim();
        if (t.isEmpty()) return -1;
        Matcher m = Pattern.compile("^(\\d{1,2})[:.,]?(\\d{2})$").matcher(t);
        if (!m.find()) {
            Matcher h = Pattern.compile("^(\\d{1,2})$").matcher(t);
            if (!h.find()) throw new IllegalArgumentException("Späteste Uhrzeit: bitte z. B. 20:45 eingeben (oder leer lassen).");
            m = Pattern.compile("^(\\d{1,2})()$").matcher(t);
            m.find();
        }
        int hh = Integer.parseInt(m.group(1));
        int mm = m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2));
        if (hh > 23 || mm > 59) throw new IllegalArgumentException("Späteste Uhrzeit: ungültige Uhrzeit.");
        return hh * 60 + mm;
    }

    /** Stunden-Feld (auch mit Komma) in Minuten umrechnen und prüfen. */
    private static int hoursToMin(EditText f, String name, double min, double max) {
        String t = f.getText().toString().trim();
        if (t.isEmpty()) t = "0";
        double h = parseDouble(t, name);
        if (h < min || h > max) {
            throw new IllegalArgumentException(name + ": erlaubt " + Fmt.hoursInput((int) Math.round(min * 60))
                    + " bis " + Fmt.hoursInput((int) Math.round(max * 60)) + " Stunden.");
        }
        return (int) Math.round(h * 60);
    }

    private void toast(String text) {
        Toast.makeText(a, text, Toast.LENGTH_LONG).show();
    }
}
