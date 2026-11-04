package de.marcel.zeitwaechter;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reiter „Arbeitszeit“: Monatsübersicht, Tagesliste nach Kalenderwochen,
 * Überstundenkonto und Auszahlungskarte.
 */
final class OvertimeTab {

    private static final String[] WD = {"MO", "DI", "MI", "DO", "FR", "SA", "SO"};
    private static final DateTimeFormatter MON_SHORT = DateTimeFormatter.ofPattern("MMM", Locale.GERMANY);

    private final MainActivity a;
    private YearMonth month = YearMonth.now();

    private final TextView tvMonth, tvMonthOt, tvLegend, tvIst, tvSoll, tvKontoEnde, tvAccTitle, tvPlaces,
            tvPayoutTitle, tvPayoutAmount, tvPayoutDetail, tvPayoutPill1, tvPayoutPill2;
    private final SegmentBarView segBar;
    private final LinearLayout dayList, accRows;

    OvertimeTab(MainActivity activity) {
        a = activity;
        tvMonth = a.findViewById(R.id.tvMonth);
        tvMonthOt = a.findViewById(R.id.tvMonthOt);
        tvLegend = a.findViewById(R.id.tvLegend);
        tvIst = a.findViewById(R.id.tvIst);
        tvSoll = a.findViewById(R.id.tvSoll);
        tvKontoEnde = a.findViewById(R.id.tvKontoEnde);
        tvPlaces = a.findViewById(R.id.tvPlaces);
        tvAccTitle = a.findViewById(R.id.tvAccTitle);
        tvPayoutTitle = a.findViewById(R.id.tvPayoutTitle);
        tvPayoutAmount = a.findViewById(R.id.tvPayoutAmount);
        tvPayoutDetail = a.findViewById(R.id.tvPayoutDetail);
        tvPayoutPill1 = a.findViewById(R.id.tvPayoutPill1);
        tvPayoutPill2 = a.findViewById(R.id.tvPayoutPill2);
        segBar = a.findViewById(R.id.segBar);
        dayList = a.findViewById(R.id.dayList);
        accRows = a.findViewById(R.id.accRows);

        a.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            month = month.minusMonths(1);
            refresh();
        });
        a.findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            month = month.plusMonths(1);
            refresh();
        });
        a.findViewById(R.id.btnExport).setOnClickListener(v -> exportCsv());
        a.findViewById(R.id.btnFab).setOnClickListener(v -> new AlertDialog.Builder(a)
                .setTitle("Neuer Eintrag")
                .setItems(new String[]{"Arbeitszeit nachtragen", "Freier Tag (Überstundenabbau)"}, (d, which) -> {
                    if (which == 0) editEntry(-1);
                    else addFreeDay();
                })
                .show());
    }

    // =====================================================================
    // Anzeige
    // =====================================================================

    void refresh() {
        List<Prefs.Entry> hist = Prefs.history(a);
        Work.Rules sur = Prefs.rules(a);
        List<Work.Day> all = Work.days(hist, sur);
        tvMonth.setText(monthName(month));
        List<Work.Day> days = Work.daysInMonth(all, month);
        Work.Month m = accountMonth(all, sur);
        refreshSummary(days, m, sur);
        refreshDayList(hist, days, sur);
        refreshAccount(m, sur);
    }

    private Work.Month accountMonth(List<Work.Day> all, Work.Rules sur) {
        YearMonth now = YearMonth.now();
        YearMonth until = month.isAfter(now) ? month : now;
        List<Work.Month> months = Work.account(all, Prefs.otStartDate(a),
                Prefs.startBalanceMin(a) * Work.MIN, Prefs.otLimitMin(a) * Work.MIN, until, sur);
        for (Work.Month x : months) if (x.ym.equals(month)) return x;
        return null;
    }

    /** Karte oben: Überstunden des Monats, farbiger Balken, Ist / Soll / Konto. */
    private void refreshSummary(List<Work.Day> days, Work.Month m, Work.Rules sur) {
        long ist = 0, soll = 0;
        for (Work.Day d : days) {
            ist += d.net;
            soll += d.soll;
        }
        long ot = ist - soll;
        tvMonthOt.setText(days.isEmpty() ? "—" : Fmt.signed(ot) + " h");
        tvMonthOt.setTextColor(a.getColor(days.isEmpty() ? R.color.text_secondary : ot >= 0 ? R.color.ok : R.color.danger));
        tvIst.setText(Fmt.hm(ist));
        tvSoll.setText(Fmt.hm(soll));
        tvKontoEnde.setText(m != null ? Fmt.signed(m.endBal) : "—");

        Work.OtSplit o = Work.split(days, sur);
        int[] colors = {a.getColor(R.color.seg_week), a.getColor(R.color.seg_sat_low), a.getColor(R.color.seg_sat_high),
                a.getColor(R.color.seg_sun), a.getColor(R.color.seg_hol)};
        long[] vals = {o.week, o.satLow, o.satHigh, o.sun, o.hol};
        String[] names = {"Mo–Fr", "Sa bis " + Fmt.hoursInput(sur.satLowMin) + " h", "Sa danach", "Sonntag", "Feiertag"};
        int[] pct = {sur.week, sur.satLow, sur.satHigh, sur.sun, sur.hol};
        segBar.set(vals, colors);

        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] <= 0) continue;
            if (sb.length() > 0) sb.append("    ");
            int st = sb.length();
            sb.append("●");
            sb.setSpan(new ForegroundColorSpan(colors[i]), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // geschützte Leerzeichen: ein Eintrag bricht nicht mitten durch
            sb.append(("\u00A0" + names[i] + " " + Fmt.hm(vals[i]) + " · " + pct[i] + " %").replace(" ", "\u00A0"));
        }
        refreshPlaces(days);
        if (sb.length() == 0) {
            sb.append("Noch keine Plus-Stunden in diesem Monat.");
        } else {
            sb.insert(0, "Plus-Stunden nach Tagesart:\n");
        }
        tvLegend.setText(sb);
    }

    /** Zusammenfassung der Arbeitsorte im Monat. */
    private void refreshPlaces(List<Work.Day> days) {
        Work.PlaceStats p = Work.placeStats(days);
        if (p.office + p.home + p.mixed == 0) {
            tvPlaces.setText("Arbeitsorte: noch keine Einträge");
            return;
        }
        SpannableStringBuilder sb = new SpannableStringBuilder("Arbeitsorte:  ");
        sb.append("🏢 " + p.office + " Büro   ");
        int st = sb.length();
        sb.append("🏠 " + p.home + " Homeoffice");
        sb.setSpan(new ForegroundColorSpan(a.getColor(R.color.home)), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (p.mixed > 0) sb.append("   🔀 " + p.mixed + " gemischt");
        if (p.homeNet > 0) sb.append("\nIm Homeoffice gearbeitet: " + Fmt.hm(p.homeNet) + " h");
        tvPlaces.setText(sb);
    }

    /** Tagesliste, neueste Tage oben, gruppiert nach Kalenderwoche. */
    private void refreshDayList(List<Prefs.Entry> hist, List<Work.Day> days, Work.Rules sur) {
        dayList.removeAllViews();

        boolean running = Prefs.active(a) && YearMonth.from(Work.day(Prefs.start(a))).equals(month);
        if (running) {
            long start = Prefs.start(a);
            long gross = Math.max(0, Work.toMinute(System.currentTimeMillis()) - Work.toMinute(start));
            dayList.addView(weekHeader("Läuft gerade"));
            boolean home = Prefs.PLACE_HOME.equals(Prefs.sessionPlace(a));
            dayList.addView(dayRow(Work.day(start), (home ? "🏠 " : "") + Fmt.time(start) + " – jetzt",
                    plain((home ? "Homeoffice · " : "") + "Netto bisher " + Fmt.hm(Work.netForGross(gross, sur))),
                    "läuft", R.drawable.bg_chip_primary, R.color.primary, false, null));
        }
        if (days.isEmpty()) {
            if (!running) {
                TextView t = text("Keine Einträge in diesem Monat.\nNeue Einträge über „+ Eintrag“ unten rechts.", 14,
                        R.color.text_secondary, false);
                t.setPadding(dp(18), dp(16), dp(18), dp(16));
                dayList.addView(t);
            }
            return;
        }

        int lastWeek = -1;
        boolean firstInWeek = true;
        for (int i = days.size() - 1; i >= 0; i--) {
            Work.Day d = days.get(i);
            int week = d.date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            if (week != lastWeek) {
                LocalDate mon = d.date.with(DayOfWeek.MONDAY);
                LocalDate sun = d.date.with(DayOfWeek.SUNDAY);
                if (mon.isBefore(month.atDay(1))) mon = month.atDay(1);
                if (sun.isAfter(month.atEndOfMonth())) sun = month.atEndOfMonth();
                dayList.addView(weekHeader("KW " + week + " · " + mon.getDayOfMonth() + ".–" + sun.getDayOfMonth() + ". "
                        + MON_SHORT.format(sun)));
                lastWeek = week;
                firstInWeek = true;
            }
            if (!firstInWeek) dayList.addView(divider());
            firstInWeek = false;
            dayList.addView(dayView(hist, d, sur));
        }
    }

    private View dayView(List<Prefs.Entry> hist, Work.Day d, Work.Rules sur) {
        Work.DayType type = Work.type(d.date, sur.state, sur.extraHolidays);
        StringBuilder times = new StringBuilder();
        long pause = 0;
        boolean manualPause = false;
        boolean free = false;
        int sessions = 0;
        for (int idx : d.indexes) {
            Prefs.Entry e = hist.get(idx);
            if (e.isFreeDay()) {
                free = true;
                continue;
            }
            sessions++;
            if (times.length() > 0) times.append(" · ");
            if (e.isHome() && d.placeType() == Work.PlaceType.GEMISCHT) times.append("🏠 ");
            times.append(Fmt.time(e.start)).append("–").append(Fmt.time(e.end));
            if (Work.day(e.end).isAfter(Work.day(e.start))) times.append("⁺¹");
            pause += Work.pauseMs(e, sur);
            manualPause |= e.pause >= 0;
        }
        if (times.length() == 0) times.append(free ? "Frei (Überstundenabbau)" : "—");

        SpannableStringBuilder meta = new SpannableStringBuilder();
        Work.PlaceType pt = d.placeType();
        if (pt == Work.PlaceType.HOMEOFFICE || pt == Work.PlaceType.GEMISCHT) {
            int st = meta.length();
            meta.append(pt == Work.PlaceType.HOMEOFFICE ? "🏠 Homeoffice" : "🔀 Büro + Homeoffice");
            meta.setSpan(new ForegroundColorSpan(a.getColor(R.color.home)), st, meta.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            meta.setSpan(new StyleSpan(Typeface.BOLD), st, meta.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            meta.append(" · ");
        }
        String tag = null;
        if (type == Work.DayType.FEIERTAG) tag = Holidays.name(d.date, sur.state, sur.extraHolidays);
        else if (type == Work.DayType.SAMSTAG) tag = "Samstag";
        else if (type == Work.DayType.SONNTAG) tag = "Sonntag";
        if (tag != null) {
            int st = meta.length();
            meta.append(tag);
            meta.setSpan(new ForegroundColorSpan(a.getColor(R.color.primary)), st, meta.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            meta.setSpan(new StyleSpan(Typeface.BOLD), st, meta.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            meta.append(" · kein Soll · ");
        }
        if (sessions > 1) meta.append(sessions + " Einsätze · ");
        else if (sessions == 1) meta.append("Pause " + Fmt.hm(pause) + (manualPause ? "" : "*") + " · ");
        meta.append("Netto " + Fmt.hm(d.net));

        boolean pos = d.diff >= 0;
        final Work.Day day = d;
        return dayRow(d.date, times.toString(), meta, Fmt.signed(d.diff),
                pos ? R.drawable.bg_chip_ok : R.drawable.bg_chip_danger, pos ? R.color.ok : R.color.danger,
                type != Work.DayType.WERKTAG, v -> openDay(day, hist));
    }

    /** Antippen: ein Eintrag → direkt bearbeiten, mehrere → erst auswählen. */
    private void openDay(Work.Day d, List<Prefs.Entry> hist) {
        if (d.indexes.size() == 1) {
            editEntry(d.indexes.get(0));
            return;
        }
        String[] items = new String[d.indexes.size()];
        for (int k = 0; k < items.length; k++) {
            Prefs.Entry e = hist.get(d.indexes.get(k));
            items[k] = e.isFreeDay() ? "Freier Tag (Abbau)"
                    : (e.isHome() ? "🏠 " : "🏢 ") + Fmt.time(e.start) + " – " + Fmt.time(e.end);
        }
        new AlertDialog.Builder(a)
                .setTitle(Fmt.dayOf(d.date) + " – welchen Einsatz?")
                .setItems(items, (dlg, which) -> editEntry(d.indexes.get(which)))
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    /** Überstundenkonto-Karte und grüne Auszahlungskarte. */
    private void refreshAccount(Work.Month m, Work.Rules sur) {
        accRows.removeAllViews();
        LocalDate startDate = Prefs.otStartDate(a);
        long limit = Prefs.otLimitMin(a) * Work.MIN;
        String limitTxt = Fmt.hoursFromMinutes(Prefs.otLimitMin(a)) + " h";
        YearMonth now = YearMonth.now();
        boolean current = month.equals(now);
        boolean future = month.isAfter(now);
        String mName = month.getMonth().getDisplayName(TextStyle.FULL, Locale.GERMANY);

        if (m == null) {
            tvAccTitle.setText("Überstundenkonto");
            TextView t = text("Dieser Monat liegt vor deinem Startdatum (" + Fmt.dateOf(startDate)
                    + ") und zählt nicht zum Konto. Startdatum und Start-Überstunden findest du unter Einstellungen → Überstunden.",
                    13.5f, R.color.text_secondary, false);
            accRows.addView(t);
            showPayout("Auszahlung " + monthName(month), "—", "Keine Abrechnung für diesen Monat.", null, null);
            return;
        }

        tvAccTitle.setText(current ? "Überstundenkonto – Stand heute" : "Überstundenkonto");
        boolean startMonth = m.from.equals(startDate);
        accRows.addView(accRow(startMonth ? "Start " + Fmt.dateOf(m.from) : "Monatsanfang", Fmt.signed(m.startBal) + " h",
                R.color.text, false));
        accRows.addView(accRow("Überstunden " + mName, Fmt.signed(m.diff) + " h",
                m.diff >= 0 ? R.color.ok : R.color.danger, false));
        View line = new View(a);
        line.setBackgroundColor(a.getColor(R.color.line));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(10);
        lp.bottomMargin = dp(6);
        accRows.addView(line, lp);
        accRows.addView(accRow(current ? "Kontostand heute" : "Kontostand Monatsende", Fmt.signed(m.endBal) + " h",
                m.endBal < 0 ? R.color.danger : R.color.text, true));
        if (!future) {
            accRows.addView(accRow("Start " + month.plusMonths(1).getMonth().getDisplayName(TextStyle.FULL, Locale.GERMANY)
                    + (current ? " (voraussichtlich)" : ""), Fmt.signed(m.carry) + " h", R.color.text, false));
        }

        // ---- Auszahlung ----
        Work.OtSplit o = m.ot;
        double rate = Prefs.hourlyRate(a);
        String title = (current ? "Voraussichtliche Auszahlung " : "Auszahlung ") + monthName(month);
        String avg = String.format(Locale.GERMANY, "Ø %.0f %% Zuschlag", o.averagePct(sur));
        if (m.payout > 0) {
            String detail = Fmt.hm(m.payout) + " h über " + limitTxt + " · brutto, vor Steuern"
                    + (current ? "\nStand heute – endgültig am Monatsende" : "");
            if (rate > 0) {
                showPayout(title, Fmt.euro(Work.payoutEuro(m, rate, sur)), detail,
                        Fmt.hm(m.payout) + " h ausgezahlt", avg);
            } else {
                showPayout(title, Fmt.hm(m.payout) + " h",
                        "Stundenlohn unter Einstellungen → Überstunden eintragen, dann steht hier der Betrag in Euro.",
                        null, avg);
            }
        } else {
            long missing = Math.max(0, limit - m.endBal);
            showPayout(title, Fmt.euro(0), (future ? "Noch keine Einträge. " : "")
                    + "Auszahlung ab " + limitTxt + " – es fehlen noch " + Fmt.hm(missing) + " h.", null, null);
        }
    }

    private void showPayout(String title, String amount, String detail, String pill1, String pill2) {
        tvPayoutTitle.setText(title);
        tvPayoutAmount.setText(amount);
        tvPayoutDetail.setText(detail);
        tvPayoutPill1.setText(pill1 == null ? "" : pill1);
        tvPayoutPill2.setText(pill2 == null ? "" : pill2);
        show(tvPayoutPill1, pill1 != null);
        show(tvPayoutPill2, pill2 != null);
    }

    private static String monthName(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.FULL, Locale.GERMANY) + " " + ym.getYear();
    }

    // ---- kleine Bausteine für Liste und Konto ----

    private View weekHeader(String t) {
        TextView tv = text(t.toUpperCase(Locale.GERMANY), 11.5f, R.color.text_secondary, true);
        tv.setLetterSpacing(0.05f);
        tv.setPadding(dp(18), dp(12), dp(18), dp(4));
        return tv;
    }

    private View divider() {
        View v = new View(a);
        v.setBackgroundColor(a.getColor(R.color.line));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMarginStart(dp(72));
        v.setLayoutParams(lp);
        return v;
    }

    private CharSequence plain(String s) {
        return s;
    }

    private View dayRow(LocalDate date, String times, CharSequence meta, String diff, int pillBg, int pillColor,
                        boolean weekend, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(9), dp(14), dp(9));
        if (click != null) {
            TypedValue tv = new TypedValue();
            a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            row.setBackgroundResource(tv.resourceId);
            row.setOnClickListener(click);
        }

        LinearLayout badge = new LinearLayout(a);
        badge.setOrientation(LinearLayout.VERTICAL);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(weekend ? R.drawable.bg_badge_we : R.drawable.bg_badge);
        int bc = weekend ? R.color.primary : R.color.text;
        TextView wd = text(WD[date.getDayOfWeek().getValue() - 1], 10.5f, weekend ? R.color.primary : R.color.text_secondary, true);
        wd.setIncludeFontPadding(false);
        wd.setGravity(Gravity.CENTER);
        TextView dn = text(String.format(Locale.GERMANY, "%02d", date.getDayOfMonth()), 17, bc, true);
        dn.setIncludeFontPadding(false);
        dn.setGravity(Gravity.CENTER);
        badge.addView(wd);
        badge.addView(dn);
        row.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout mid = new LinearLayout(a);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView t1 = text(times, 14.5f, R.color.text, true);
        t1.setFontFeatureSettings("tnum");
        TextView t2 = new TextView(a);
        t2.setText(meta);
        t2.setTextSize(12.5f);
        t2.setTextColor(a.getColor(R.color.text_secondary));
        mid.addView(t1);
        mid.addView(t2);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        mp.setMarginStart(dp(12));
        mp.setMarginEnd(dp(8));
        row.addView(mid, mp);

        TextView pill = text(diff, 13, pillColor, true);
        pill.setBackgroundResource(pillBg);
        pill.setPadding(dp(10), dp(5), dp(10), dp(5));
        pill.setFontFeatureSettings("tnum");
        row.addView(pill);
        return row;
    }

    private View accRow(String label, String value, int valueColor, boolean bold) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView l = text(label, 14.5f, bold ? R.color.text : R.color.text_secondary, bold);
        TextView v = text(value, 14.5f, valueColor, true);
        v.setFontFeatureSettings("tnum");
        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v);
        return row;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(a.getColor(color));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    // =====================================================================
    // Einträge bearbeiten
    // =====================================================================

    /** Eintrag bearbeiten (index >= 0) oder neu anlegen (index = -1). */
    private void editEntry(int index) {
        List<Prefs.Entry> hist = Prefs.history(a);
        final Prefs.Entry e = index >= 0 && index < hist.size() ? hist.get(index) : null;
        if (e != null && e.isFreeDay()) {
            freeDayDialog(index, e);
            return;
        }

        ZoneId zone = ZoneId.systemDefault();
        final LocalDate[] date = new LocalDate[1];
        final LocalTime[] from = new LocalTime[1];
        final LocalTime[] to = new LocalTime[1];
        if (e != null) {
            ZonedDateTime s = Fmt.zoned(e.start);
            ZonedDateTime t = Fmt.zoned(e.end);
            date[0] = s.toLocalDate();
            from[0] = LocalTime.of(s.getHour(), s.getMinute());
            to[0] = LocalTime.of(t.getHour(), t.getMinute());
        } else {
            date[0] = defaultDate();
            from[0] = LocalTime.of(6, 30);
            to[0] = LocalTime.of(15, 0);
        }

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        final Button bDate = plainButton();
        final Button bFrom = plainButton();
        final Button bTo = plainButton();
        final Button bPlace = plainButton();
        final String[] place = {e != null ? e.place : Prefs.PLACE_OFFICE};
        TextView lbl = new TextView(a);
        lbl.setText("Pause in Minuten (leer lassen = automatisch: ab 6 h, max. 45 Min.)");
        lbl.setTextColor(a.getColor(R.color.text_secondary));
        lbl.setPadding(0, dp(10), 0, 0);
        final EditText etPause = new EditText(a);
        etPause.setBackgroundResource(R.drawable.bg_field);
        etPause.setPadding(dp(14), dp(12), dp(14), dp(12));
        etPause.setTextColor(a.getColor(R.color.text));
        etPause.setInputType(InputType.TYPE_CLASS_NUMBER);
        etPause.setHint("automatisch");
        if (e != null && e.pause >= 0) etPause.setText(String.valueOf(e.pause));

        final Runnable update = () -> {
            bDate.setText("Datum:  " + Fmt.day(date[0].atStartOfDay(zone).toInstant().toEpochMilli())
                    + date[0].getYear());
            bFrom.setText("Kommen:  " + from[0]);
            bTo.setText("Gehen:  " + to[0] + (!to[0].isAfter(from[0]) ? "  (nächster Tag)" : ""));
        };
        update.run();

        bDate.setOnClickListener(v -> new DatePickerDialog(a, (view, y, m, d) -> {
            date[0] = LocalDate.of(y, m + 1, d);
            update.run();
        }, date[0].getYear(), date[0].getMonthValue() - 1, date[0].getDayOfMonth()).show());
        bFrom.setOnClickListener(v -> new TimePickerDialog(a, (view, h, m) -> {
            from[0] = LocalTime.of(h, m);
            update.run();
        }, from[0].getHour(), from[0].getMinute(), true).show());
        bTo.setOnClickListener(v -> new TimePickerDialog(a, (view, h, m) -> {
            to[0] = LocalTime.of(h, m);
            update.run();
        }, to[0].getHour(), to[0].getMinute(), true).show());

        box.addView(bDate);
        box.addView(bFrom);
        box.addView(bTo);
        box.addView(bPlace);
        final Runnable updPlace = () -> bPlace.setText("Ort:  " + (Prefs.PLACE_HOME.equals(place[0])
                ? "🏠 Homeoffice" : "🏢 Büro") + "   (tippen zum Wechseln)");
        updPlace.run();
        bPlace.setOnClickListener(v -> {
            place[0] = Prefs.PLACE_HOME.equals(place[0]) ? Prefs.PLACE_OFFICE : Prefs.PLACE_HOME;
            updPlace.run();
        });
        box.addView(lbl);
        box.addView(etPause);

        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle(e == null ? "Eintrag hinzufügen" : "Eintrag bearbeiten")
                .setView(box)
                .setPositiveButton("Speichern", (d, w) -> {
                    long start = date[0].atTime(from[0]).atZone(zone).toInstant().toEpochMilli();
                    long end = date[0].atTime(to[0]).atZone(zone).toInstant().toEpochMilli();
                    if (end <= start) end += 24 * Work.HOUR; // Nachtdienst: Ende am nächsten Tag
                    int pause = -1;
                    String p = etPause.getText().toString().trim();
                    if (!p.isEmpty()) {
                        try {
                            pause = Integer.parseInt(p);
                        } catch (NumberFormatException ex) {
                            pause = -1;
                        }
                        if (pause < 0 || pause > 600) {
                            toast("Pause: 0 bis 600 Minuten.");
                            return;
                        }
                    }
                    Prefs.Entry ne = new Prefs.Entry(start, end,
                            e == null ? "nachgetragen" : "korrigiert", pause, place[0]);
                    List<Prefs.Entry> list = Prefs.history(a);
                    if (e != null && index < list.size()) list.set(index, ne);
                    else list.add(ne);
                    saveAndRefresh(list);
                    month = YearMonth.from(date[0]);
                    refresh();
                    toast("Gespeichert ✔");
                })
                .setNegativeButton("Abbrechen", null);
        if (e != null) b.setNeutralButton("Löschen", (d, w) -> confirmDelete(index, e));
        b.show();
    }

    private void confirmDelete(int index, Prefs.Entry e) {
        new AlertDialog.Builder(a)
                .setTitle("Eintrag löschen?")
                .setMessage(Fmt.day(e.start) + "  " + Fmt.time(e.start) + "–" + Fmt.time(e.end))
                .setPositiveButton("Löschen", (d, w) -> {
                    List<Prefs.Entry> list = Prefs.history(a);
                    if (index < list.size()) list.remove(index);
                    saveAndRefresh(list);
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    /** Freier Tag zum Überstundenabbau: zählt die Sollzeit als Minus. */
    private void addFreeDay() {
        LocalDate d0 = defaultDate();
        new DatePickerDialog(a, (view, y, m, d) -> {
            LocalDate date = LocalDate.of(y, m + 1, d);
            long noon = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            List<Prefs.Entry> list = Prefs.history(a);
            list.add(new Prefs.Entry(noon, noon, Prefs.SOURCE_FREE, 0));
            saveAndRefresh(list);
            month = YearMonth.from(date);
            refresh();
            long soll = Work.sollFor(date, Prefs.rules(a));
            toast(soll > 0
                    ? "Freier Tag eingetragen: " + Fmt.signed(-soll) + " h"
                    : "Hinweis: An Wochenenden und Feiertagen gibt es keine Sollzeit – der freie Tag zählt 0 h.");
        }, d0.getYear(), d0.getMonthValue() - 1, d0.getDayOfMonth()).show();
    }

    private void freeDayDialog(int index, Prefs.Entry e) {
        new AlertDialog.Builder(a)
                .setTitle("Freier Tag (Überstundenabbau)")
                .setMessage(Fmt.date(e.start) + ": zählt " + Fmt.signed(-Work.sollFor(Work.day(e.start), Prefs.rules(a)))
                        + " h auf dem Überstundenkonto.")
                .setNeutralButton("Löschen", (d, w) -> confirmDelete(index, e))
                .setNegativeButton("Schließen", null)
                .show();
    }

    private void saveAndRefresh(List<Prefs.Entry> list) {
        Prefs.setHistory(a, list);
        SessionManager.historyChanged(a); // frühere Einsätze beeinflussen die heutige Grenze
        refresh();
        a.refreshAll();
    }

    private LocalDate defaultDate() {
        LocalDate today = LocalDate.now();
        return YearMonth.from(today).equals(month) ? today : month.atDay(1);
    }

    // =====================================================================
    // Export
    // =====================================================================

    private void exportCsv() {
        List<Prefs.Entry> hist = Prefs.history(a);
        if (hist.isEmpty()) {
            toast("Noch keine Einträge.");
            return;
        }
        Work.Rules sur = Prefs.rules(a);
        List<Work.Day> days = Work.days(hist, sur);
        StringBuilder csv = new StringBuilder(
                "Datum;Kommen;Gehen;Pause Min;Netto h;Soll h (Tag);Differenz h (Tag);Art;Ort\n");
        for (Work.Day d : days) {
            for (int k = 0; k < d.indexes.size(); k++) {
                Prefs.Entry e = hist.get(d.indexes.get(k));
                boolean last = k == d.indexes.size() - 1;
                csv.append(Fmt.date(e.start)).append(';')
                        .append(e.isFreeDay() ? "" : Fmt.time(e.start)).append(';')
                        .append(e.isFreeDay() ? "" : Fmt.time(e.end)).append(';')
                        .append(Work.pauseMs(e, sur) / Work.MIN).append(';')
                        .append(Fmt.hoursDecimal(Work.netMs(e, sur))).append(';')
                        .append(last ? Fmt.hoursDecimal(d.soll) : "").append(';')
                        .append(last ? String.format(Locale.GERMANY, "%.2f", d.diff / (double) Work.HOUR) : "")
                        .append(';')
                        .append(e.isFreeDay() ? "Überstundenabbau" : e.source).append(';')
                        .append(e.isFreeDay() ? "" : e.isHome() ? "Homeoffice" : "Büro").append('\n');
            }
        }
        List<Work.Month> months = Work.account(days, Prefs.otStartDate(a),
                Prefs.startBalanceMin(a) * Work.MIN, Prefs.otLimitMin(a) * Work.MIN, YearMonth.now(), sur);
        csv.append("\nMonat;Anfang h;Überstunden h;Ende h;Ausgezahlt h;Ø Zuschlag %;Betrag brutto €\n");
        for (Work.Month m : months) {
            csv.append(m.ym).append(';')
                    .append(String.format(Locale.GERMANY, "%.2f;%.2f;%.2f;%.2f;%.1f;%.2f",
                            m.startBal / (double) Work.HOUR, m.diff / (double) Work.HOUR,
                            m.endBal / (double) Work.HOUR, m.payout / (double) Work.HOUR,
                            m.ot.averagePct(sur), Work.payoutEuro(m, Prefs.hourlyRate(a), sur)))
                    .append('\n');
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Zeitwächter Arbeitszeit");
        send.putExtra(Intent.EXTRA_TEXT, csv.toString());
        a.startActivity(Intent.createChooser(send, "Arbeitszeit teilen"));
    }

    private Button plainButton() {
        Button b = new Button(a);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setBackgroundResource(R.drawable.bg_btn_tonal);
        b.setTextColor(a.getColor(R.color.text));
        b.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        b.setPadding(dp(18), 0, dp(18), 0);
        return b;
    }

    private int dp(int v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String text) {
        Toast.makeText(a, text, Toast.LENGTH_LONG).show();
    }

    /** Sichtbarkeit umschalten. */
    static void show(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
