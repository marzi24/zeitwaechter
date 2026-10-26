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

    private final TextView tvMonth, tvMonthSum, tvBalance, tvBalanceInfo, tvPayout, tvAdjustments;
    private final TableLayout table;
    private final EditText etSoll, etStartBal, etLimit, etRate, etSurcharge;
    private final Button btnBookPayout;

    OvertimeTab(MainActivity activity) {
        a = activity;
        tvMonth = a.findViewById(R.id.tvMonth);
        tvMonthSum = a.findViewById(R.id.tvMonthSum);
        tvBalance = a.findViewById(R.id.tvBalance);
        tvBalanceInfo = a.findViewById(R.id.tvBalanceInfo);
        tvPayout = a.findViewById(R.id.tvPayout);
        tvAdjustments = a.findViewById(R.id.tvAdjustments);
        table = a.findViewById(R.id.tableMonth);
        etSoll = a.findViewById(R.id.etSoll);
        etStartBal = a.findViewById(R.id.etStartBal);
        etLimit = a.findViewById(R.id.etLimit);
        etRate = a.findViewById(R.id.etRate);
        etSurcharge = a.findViewById(R.id.etSurcharge);
        btnBookPayout = a.findViewById(R.id.btnBookPayout);

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
        a.findViewById(R.id.btnSaveOt).setOnClickListener(v -> saveSettings());
        btnBookPayout.setOnClickListener(v -> bookPayout());
        a.findViewById(R.id.btnDeleteAdj).setOnClickListener(v -> deleteLastAdjustment());

        fillSettings();
    }

    // =====================================================================
    // Anzeige
    // =====================================================================

    void refresh() {
        List<Prefs.Entry> hist = Prefs.history(a);
        List<Work.Day> all = Work.days(hist, Prefs.sollMin(a));
        refreshTable(hist, all);
        refreshBalance(all);
        refreshAdjustments();
    }

    private void refreshTable(List<Prefs.Entry> hist, List<Work.Day> all) {
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
                String date = k == 0 ? Fmt.day(e.start) : "";
                String time, pause, net;
                if (e.isFreeDay()) {
                    time = "frei (Abbau)";
                    pause = "";
                    net = "0:00";
                } else {
                    time = Fmt.time(e.start) + "–" + Fmt.time(e.end)
                            + (Work.day(e.end).isAfter(Work.day(e.start)) ? "⁺¹" : "");
                    pause = Fmt.hm(Work.pauseMs(e)) + (e.pause < 0 ? "*" : "");
                    net = Fmt.hm(Work.netMs(e));
                }
                String diff = last ? Fmt.signed(d.diff) : "";
                TableRow r = row(bg, false, text, date, time, pause, net, diff);
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
                    Fmt.day(start), Fmt.time(start) + "–läuft", "", Fmt.hm(Fmt.netMs(gross)), ""));
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

    private void refreshBalance(List<Work.Day> all) {
        List<Prefs.Adjustment> adj = Prefs.adjustments(a);
        int startBal = Prefs.startBalanceMin(a);
        int limit = Prefs.otLimitMin(a);
        long bal = Work.balanceMs(all, startBal, adj);

        long fromDays = 0;
        for (Work.Day d : all) fromDays += d.diff;
        long fromAdj = 0;
        for (Prefs.Adjustment x : adj) fromAdj += x.minutes * Work.MIN;

        tvBalance.setText(Fmt.signed(bal) + " h");
        tvBalance.setTextColor(a.getColor(bal < 0 ? R.color.danger : R.color.text));
        tvBalanceInfo.setText("Übertrag " + Fmt.signed(startBal * Work.MIN) + " h"
                + "  ·  aus Einträgen " + Fmt.signed(fromDays) + " h"
                + (adj.isEmpty() ? "" : "  ·  Auszahlungen " + Fmt.signed(fromAdj) + " h"));

        long pay = Work.payoutMs(bal, limit);
        double rate = Prefs.hourlyRate(a);
        int pct = Prefs.surchargePct(a);
        String limitTxt = Fmt.hoursFromMinutes(limit) + " h";
        StringBuilder sb = new StringBuilder();
        if (pay > 0) {
            double euro = Work.payoutEuro(pay, rate, pct);
            sb.append("Über ").append(limitTxt).append(": ").append(Fmt.hm(pay)).append(" h")
                    .append(String.format(Locale.GERMANY, " (= %.2f h)", pay / (double) Work.HOUR))
                    .append("\nWird ausgezahlt: ca. ").append(Fmt.euro(euro)).append(" brutto");
            if (rate > 0) {
                sb.append(String.format(Locale.GERMANY, "\n%.2f h × %s", pay / (double) Work.HOUR, Fmt.euro(rate)));
                if (pct > 0) sb.append(" + ").append(pct).append(" % Zuschlag");
            } else {
                sb.append("\n→ Stundenlohn unten in den Einstellungen eintragen.");
            }
        } else {
            sb.append("Auszahlung ab ").append(limitTxt).append(" Kontostand.\nBis dahin fehlen noch ")
                    .append(Fmt.hm(limit * Work.MIN - bal)).append(" h.\nAktuell: ").append(Fmt.euro(0));
        }
        tvPayout.setText(sb.toString());
        btnBookPayout.setEnabled(pay > 0);
    }

    private void refreshAdjustments() {
        List<Prefs.Adjustment> adj = Prefs.adjustments(a);
        if (adj.isEmpty()) {
            tvAdjustments.setText("Noch keine.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = adj.size() - 1; i >= 0; i--) {
            Prefs.Adjustment x = adj.get(i);
            sb.append(Fmt.date(x.time)).append("  ").append(Fmt.signed(x.minutes * Work.MIN)).append(" h  ")
                    .append(x.note);
            if (i > 0) sb.append('\n');
        }
        tvAdjustments.setText(sb.toString());
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
            toast("Freier Tag eingetragen: " + Fmt.signed(-Prefs.sollMin(a) * Work.MIN) + " h");
        }, d0.getYear(), d0.getMonthValue() - 1, d0.getDayOfMonth()).show();
    }

    private void freeDayDialog(int index, Prefs.Entry e) {
        new AlertDialog.Builder(a)
                .setTitle("Freier Tag (Überstundenabbau)")
                .setMessage(Fmt.date(e.start) + ": zählt " + Fmt.signed(-Prefs.sollMin(a) * Work.MIN)
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
    // Auszahlung
    // =====================================================================

    private void bookPayout() {
        List<Work.Day> all = Work.days(Prefs.history(a), Prefs.sollMin(a));
        long bal = Work.balanceMs(all, Prefs.startBalanceMin(a), Prefs.adjustments(a));
        long pay = Work.payoutMs(bal, Prefs.otLimitMin(a));
        if (pay <= 0) return;
        final int payMin = (int) (pay / Work.MIN);
        final double euro = Work.payoutEuro(pay, Prefs.hourlyRate(a), Prefs.surchargePct(a));
        new AlertDialog.Builder(a)
                .setTitle("Auszahlung verbuchen?")
                .setMessage(Fmt.hm(pay) + " h (ca. " + Fmt.euro(euro) + " brutto) werden vom Konto abgezogen, "
                        + "z. B. wenn sie mit dem Gehalt ausgezahlt wurden.\n\nKontostand danach: "
                        + Fmt.signed(bal - payMin * Work.MIN) + " h")
                .setPositiveButton("Verbuchen", (d, w) -> {
                    Prefs.addAdjustment(a, new Prefs.Adjustment(System.currentTimeMillis(), -payMin,
                            "Auszahlung ca. " + Fmt.euro(euro)));
                    refresh();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void deleteLastAdjustment() {
        List<Prefs.Adjustment> adj = Prefs.adjustments(a);
        if (adj.isEmpty()) {
            toast("Keine Buchungen vorhanden.");
            return;
        }
        Prefs.Adjustment x = adj.get(adj.size() - 1);
        new AlertDialog.Builder(a)
                .setTitle("Letzte Buchung löschen?")
                .setMessage(Fmt.date(x.time) + "  " + Fmt.signed(x.minutes * Work.MIN) + " h  " + x.note)
                .setPositiveButton("Löschen", (d, w) -> {
                    Prefs.removeLastAdjustment(a);
                    refresh();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    // =====================================================================
    // Einstellungen
    // =====================================================================

    private void fillSettings() {
        etSoll.setText(Fmt.hoursInput(Prefs.sollMin(a)));
        etStartBal.setText(Fmt.hoursInput(Prefs.startBalanceMin(a)));
        etLimit.setText(Fmt.hoursInput(Prefs.otLimitMin(a)));
        double rate = Prefs.hourlyRate(a);
        etRate.setText(rate > 0 ? String.format(Locale.GERMANY, "%.2f", rate) : "");
        etSurcharge.setText(String.valueOf(Prefs.surchargePct(a)));
    }

    private void saveSettings() {
        try {
            int soll = hoursToMin(etSoll, "Sollarbeitszeit", 0.5, 24);
            int startBal = hoursToMin(etStartBal, "Übertrag", -2000, 2000);
            int limit = hoursToMin(etLimit, "Auszahlungsgrenze", 0, 2000);
            String rateTxt = etRate.getText().toString().trim();
            double rate = rateTxt.isEmpty() ? 0 : num(rateTxt, "Stundenlohn");
            if (rate < 0 || rate > 1000) throw new IllegalArgumentException("Stundenlohn: 0 bis 1000 €.");
            String sTxt = etSurcharge.getText().toString().trim();
            int pct = sTxt.isEmpty() ? 0 : (int) Math.round(num(sTxt, "Zuschlag"));
            if (pct < 0 || pct > 300) throw new IllegalArgumentException("Zuschlag: 0 bis 300 %.");
            Prefs.saveOvertimeSettings(a, soll, startBal, limit, rate, pct);
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
            return;
        }
        fillSettings();
        refresh();
        toast("Gespeichert ✔");
    }

    private static int hoursToMin(EditText f, String name, double min, double max) {
        String t = f.getText().toString().trim();
        if (t.isEmpty()) t = "0";
        double h = num(t, name);
        if (h < min || h > max) {
            throw new IllegalArgumentException(name + ": erlaubt " + Fmt.hoursInput((int) (min * 60))
                    + " bis " + Fmt.hoursInput((int) (max * 60)) + " Stunden.");
        }
        return (int) Math.round(h * 60);
    }

    private static double num(String text, String name) {
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + ": bitte eine Zahl eingeben.");
        }
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
        List<Work.Day> days = Work.days(hist, Prefs.sollMin(a));
        StringBuilder csv = new StringBuilder(
                "Datum;Kommen;Gehen;Pause Min;Netto h;Soll h (Tag);Differenz h (Tag);Art\n");
        for (Work.Day d : days) {
            for (int k = 0; k < d.indexes.size(); k++) {
                Prefs.Entry e = hist.get(d.indexes.get(k));
                boolean last = k == d.indexes.size() - 1;
                csv.append(Fmt.date(e.start)).append(';')
                        .append(e.isFreeDay() ? "" : Fmt.time(e.start)).append(';')
                        .append(e.isFreeDay() ? "" : Fmt.time(e.end)).append(';')
                        .append(Work.pauseMs(e) / Work.MIN).append(';')
                        .append(Fmt.hoursDecimal(Work.netMs(e))).append(';')
                        .append(last ? Fmt.hoursDecimal(d.soll) : "").append(';')
                        .append(last ? String.format(Locale.GERMANY, "%.2f", d.diff / (double) Work.HOUR) : "")
                        .append(';')
                        .append(e.isFreeDay() ? "Überstundenabbau" : e.source).append('\n');
            }
        }
        long bal = Work.balanceMs(days, Prefs.startBalanceMin(a), Prefs.adjustments(a));
        csv.append("\nÜberstundenkonto;;;;")
                .append(String.format(Locale.GERMANY, "%.2f", bal / (double) Work.HOUR)).append('\n');

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
