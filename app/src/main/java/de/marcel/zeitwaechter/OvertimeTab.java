package de.marcel.zeitwaechter;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Reiter „Arbeitszeit“: Monatstabelle, Einträge bearbeiten/hinzufügen,
 * Überstundenkonto und Auszahlung über der Grenze (Standard 40 h).
 */
final class OvertimeTab {

    private final MainActivity a;
    private YearMonth month = YearMonth.now();

    private final TextView tvMonth, tvMonthSum, tvAccTitle, tvBalance, tvBalanceInfo, tvOtSplit,
            tvPayoutTitle, tvPayoutAmount, tvPayoutDetail;
    private final TableLayout table;

    OvertimeTab(MainActivity activity) {
        a = activity;
        tvMonth = a.findViewById(R.id.tvMonth);
        tvMonthSum = a.findViewById(R.id.tvMonthSum);
        tvAccTitle = a.findViewById(R.id.tvAccTitle);
        tvBalance = a.findViewById(R.id.tvBalance);
        tvBalanceInfo = a.findViewById(R.id.tvBalanceInfo);
        tvOtSplit = a.findViewById(R.id.tvOtSplit);
        tvPayoutTitle = a.findViewById(R.id.tvPayoutTitle);
        tvPayoutAmount = a.findViewById(R.id.tvPayoutAmount);
        tvPayoutDetail = a.findViewById(R.id.tvPayoutDetail);
        table = a.findViewById(R.id.tableMonth);

        a.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            month = month.minusMonths(1);
            refresh();
        });
        a.findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            month = month.plusMonths(1);
            refresh();
        });
        a.findViewById(R.id.btnAddEntry).setOnClickListener(v -> editEntry(-1));
        a.findViewById(R.id.btnAddFree).setOnClickListener(v -> addFreeDay());
        a.findViewById(R.id.btnExport).setOnClickListener(v -> exportCsv());
    }

    // =====================================================================
    // Anzeige
    // =====================================================================

    void refresh() {
        List<Prefs.Entry> hist = Prefs.history(a);
        Work.Rules sur = Prefs.rules(a);
        List<Work.Day> all = Work.days(hist, sur);
        refreshTable(hist, all, sur);
        refreshAccount(all, sur);
    }

    private void refreshTable(List<Prefs.Entry> hist, List<Work.Day> all, Work.Rules sur) {
        String name = month.getMonth().getDisplayName(TextStyle.FULL, Locale.GERMANY);
        tvMonth.setText(name + " " + month.getYear());

        table.removeAllViews();
        int text = a.getColor(R.color.text);
        int secondary = a.getColor(R.color.text_secondary);
        table.addView(row(0, true, secondary,
                "Datum", "Zeit", "Pause", "Netto", "+/−"));

        List<Work.Day> days = Work.daysInMonth(all, month);
        boolean alt = false;
        long ist = 0, soll = 0;
        for (Work.Day d : days) {
            int bg = alt ? a.getColor(R.color.row_alt) : 0;
            alt = !alt;
            ist += d.net;
            soll += d.soll;
            for (int k = 0; k < d.indexes.size(); k++) {
                final int idx = d.indexes.get(k);
                Prefs.Entry e = hist.get(idx);
                boolean last = k == d.indexes.size() - 1;
                Work.DayType type = Work.type(d.date, sur.state, sur.extraHolidays);
                String date = k == 0
                        ? Fmt.day(e.start) + (type == Work.DayType.FEIERTAG ? " F" : "")
                        : "";
                String time, pause, net;
                if (e.isFreeDay()) {
                    time = "frei (Abbau)";
                    pause = "";
                    net = "0:00";
                } else {
                    time = Fmt.time(e.start) + "–" + Fmt.time(e.end)
                            + (Work.day(e.end).isAfter(Work.day(e.start)) ? "⁺¹" : "");
                    pause = Fmt.hm(Work.pauseMs(e, sur)) + (e.pause < 0 ? "*" : "");
                    net = Fmt.hm(Work.netMs(e, sur));
                }
                String diff = last ? Fmt.signed(d.diff) : "";
                TableRow r = row(bg, false, text, date, time, pause, net, diff);
                if (k == 0 && type != Work.DayType.WERKTAG) {
                    ((TextView) r.getChildAt(0)).setTextColor(a.getColor(R.color.primary));
                }
                if (last) {
                    TextView dv = (TextView) r.getChildAt(4);
                    dv.setTextColor(a.getColor(d.diff >= 0 ? R.color.ok : R.color.danger));
                    dv.setTypeface(Typeface.DEFAULT_BOLD);
                }
                r.setOnClickListener(v -> editEntry(idx));
                table.addView(r);
            }
        }

        // Laufender Einsatz (noch nicht in den Summen)
        boolean runningHere = Prefs.active(a) && YearMonth.from(Work.day(Prefs.start(a))).equals(month);
        if (runningHere) {
            long start = Prefs.start(a);
            long gross = Math.max(0, System.currentTimeMillis() - start);
            table.addView(row(0, false, secondary,
                    Fmt.day(start), Fmt.time(start) + "–läuft", "", Fmt.hm(Work.netForGross(gross, sur)), ""));
        }
        if (days.isEmpty() && !runningHere) {
            TableRow r = new TableRow(a);
            TextView t = cell("Keine Einträge in diesem Monat.", false, secondary, Gravity.START);
            r.addView(t);
            TableRow.LayoutParams lp = new TableRow.LayoutParams();
            lp.span = 5;
            t.setLayoutParams(lp);
            table.addView(r);
        }

        tvMonthSum.setText("Arbeitstage: " + days.size()
                + "   Ist: " + Fmt.hm(ist) + " h   Soll: " + Fmt.hm(soll) + " h\n"
                + "Überstunden diesen Monat: " + Fmt.signed(ist - soll) + " h"
                + (runningHere ? "\n(laufender Einsatz wird nach dem Ausstempeln eingerechnet)" : ""));
    }

    /** Überstundenkonto des angezeigten Monats mit Zuschlägen und Auszahlung. */
    private void refreshAccount(List<Work.Day> all, Work.Rules sur) {
        LocalDate startDate = Prefs.otStartDate(a);
        long limit = Prefs.otLimitMin(a) * Work.MIN;
        String limitTxt = Fmt.hoursFromMinutes(Prefs.otLimitMin(a)) + " h";
        YearMonth now = YearMonth.now();
        YearMonth until = month.isAfter(now) ? month : now;
        List<Work.Month> months = Work.account(all, startDate,
                Prefs.startBalanceMin(a) * Work.MIN, limit, until, sur);

        Work.Month m = null;
        for (Work.Month x : months) if (x.ym.equals(month)) m = x;

        if (m == null) {
            tvAccTitle.setText("Überstundenkonto");
            tvBalance.setText("—");
            tvBalance.setTextColor(a.getColor(R.color.text_secondary));
            tvBalanceInfo.setText("Dieser Monat liegt vor deinem Startdatum (" + Fmt.dateOf(startDate)
                    + ") und zählt nicht zum Konto.\nStartdatum und Start-Überstunden: ⚙ Einstellungen.");
            tvOtSplit.setText("");
            showPayout("Auszahlung " + monthName(month), "—", "Keine Abrechnung für diesen Monat.");
            return;
        }

        boolean current = month.equals(now);
        boolean future = month.isAfter(now);
        tvAccTitle.setText(current ? "Überstundenkonto – Stand heute" : "Überstundenkonto – Monatsende");
        tvBalance.setText(Fmt.signed(m.endBal) + " h");
        tvBalance.setTextColor(a.getColor(m.endBal < 0 ? R.color.danger : R.color.text));

        boolean startMonth = m.from.getDayOfMonth() != 1 || m.from.equals(startDate);
        String startLbl = startMonth ? "Start " + Fmt.dateOf(m.from) : "Monatsanfang";
        tvBalanceInfo.setText(
                pad(startLbl) + Fmt.signed(m.startBal) + " h\n"
                        + pad("Überstunden Monat") + Fmt.signed(m.diff) + " h\n"
                        + pad(current ? "= Stand heute" : "= Monatsende") + Fmt.signed(m.endBal) + " h");

        // Aufteilung nach Zuschlag
        Work.OtSplit o = m.ot;
        if (o.total() > 0) {
            StringBuilder sb = new StringBuilder("Überstunden nach Zuschlag:");
            appendSplit(sb, "Mo–Fr", o.week, sur.week);
            appendSplit(sb, "Sa bis " + Fmt.hoursInput(sur.satLowMin) + " h", o.satLow, sur.satLow);
            appendSplit(sb, "Sa danach", o.satHigh, sur.satHigh);
            appendSplit(sb, "Sonntag", o.sun, sur.sun);
            appendSplit(sb, "Feiertag", o.hol, sur.hol);
            sb.append(String.format(Locale.GERMANY, "\n%s Ø %.1f %%", pad("Durchschnitt"), o.averagePct(sur)));
            tvOtSplit.setText(sb.toString());
        } else {
            tvOtSplit.setText("Noch keine Überstunden in diesem Monat.");
        }

        // Auszahlung – groß unten auf der Karte
        double rate = Prefs.hourlyRate(a);
        String title = (current ? "Voraussichtliche Auszahlung " : "Auszahlung ") + monthName(month);
        if (m.payout > 0) {
            double euro = Work.payoutEuro(m, rate, sur);
            String detail = Fmt.hm(m.payout) + " h über " + limitTxt
                    + String.format(Locale.GERMANY, " · Ø %.1f %% Zuschlag", o.averagePct(sur))
                    + (rate > 0 ? String.format(Locale.GERMANY, "\n%.2f h × %s × %.3f",
                            m.payout / (double) Work.HOUR, Fmt.euro(rate), 1 + o.averagePct(sur) / 100.0) : "")
                    + "\nbrutto, vor Steuern und Abgaben"
                    + (current ? "\nStand heute – endgültig am Monatsende" : "")
                    + "\nNächster Monat startet mit " + Fmt.signed(m.carry) + " h";
            if (rate > 0) {
                showPayout(title, "ca. " + Fmt.euro(euro), detail);
            } else {
                showPayout(title, Fmt.hm(m.payout) + " h",
                        "Stundenlohn unter ⚙ Einstellungen eintragen,\ndann steht hier der Betrag in Euro.");
            }
        } else {
            long missing = limit - m.endBal;
            String detail = (future ? "Noch keine Einträge.\n" : "")
                    + "Auszahlung ab " + limitTxt + " – es fehlen noch " + Fmt.hm(missing) + " h"
                    + (!current && !future ? "\nNächster Monat startet mit " + Fmt.signed(m.carry) + " h" : "");
            showPayout(title, Fmt.euro(0), detail);
        }
    }

    private void showPayout(String title, String amount, String detail) {
        tvPayoutTitle.setText(title);
        tvPayoutAmount.setText(amount);
        tvPayoutDetail.setText(detail);
    }

    private static String monthName(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.FULL, Locale.GERMANY) + " " + ym.getYear();
    }

    private static void appendSplit(StringBuilder sb, String label, long ms, int pct) {
        if (ms <= 0) return;
        sb.append('\n').append(pad(label)).append(String.format(Locale.GERMANY, "%6s h  %3d %%", Fmt.hm(ms), pct));
    }

    /** Text auf feste Breite auffüllen (für saubere Spalten in Monospace). */
    private static String pad(String s) {
        StringBuilder b = new StringBuilder(s);
        while (b.length() < 18) b.append(' ');
        return b.toString();
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
        TextView lbl = new TextView(a);
        lbl.setText("Pause in Minuten (leer lassen = automatisch nach Arbeitszeitgesetz)");
        lbl.setTextColor(a.getColor(R.color.text_secondary));
        lbl.setPadding(0, dp(10), 0, 0);
        final EditText etPause = new EditText(a);
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
                            e == null ? "nachgetragen" : "korrigiert", pause);
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
                "Datum;Kommen;Gehen;Pause Min;Netto h;Soll h (Tag);Differenz h (Tag);Art\n");
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
                        .append(e.isFreeDay() ? "Überstundenabbau" : e.source).append('\n');
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

    // =====================================================================
    // Hilfsfunktionen für die Tabelle
    // =====================================================================

    private TableRow row(int bg, boolean bold, int color, String... cells) {
        TableRow r = new TableRow(a);
        if (bg != 0) r.setBackgroundColor(bg);
        for (int i = 0; i < cells.length; i++) {
            int gravity = i <= 1 ? Gravity.START : Gravity.END;
            r.addView(cell(cells[i], bold, color, gravity));
        }
        return r;
    }

    private TextView cell(String text, boolean bold, int color, int gravity) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(color);
        t.setGravity(gravity);
        t.setPadding(dp(4), dp(7), dp(4), dp(7));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button plainButton() {
        Button b = new Button(a);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return b;
    }

    private int dp(int v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String text) {
        Toast.makeText(a, text, Toast.LENGTH_LONG).show();
    }

    /** Sichtbarkeit für den Reiter-Wechsel. */
    static void show(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
