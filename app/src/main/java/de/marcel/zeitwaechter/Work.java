package de.marcel.zeitwaechter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Alle Berechnungen rund um Arbeitszeit, Tagesgrenze und Überstunden.
 * Enthält bewusst keinen Android-Code, damit man sie einfach testen kann.
 */
final class Work {

    private Work() {
    }

    static final long MIN = 60_000L;
    static final long HOUR = 3_600_000L;

    /** Ab dieser Ruhezeit beginnt ein neuer Arbeitstag (§ 5 ArbZG: 11 Stunden). */
    static final long REST_NEW_DAY = 11 * HOUR;
    /** Lücken zwischen zwei Einsätzen zählen ab 15 Minuten als Pause (§ 4 ArbZG). */
    static final long MIN_BREAK_BLOCK = 15 * MIN;

    // =====================================================================
    // Ein einzelner Eintrag
    // =====================================================================

    /** Anwesenheit (Kommen bis Gehen). Freie Tage haben 0. */
    static long grossMs(Prefs.Entry e) {
        if (e.isFreeDay()) return 0;
        return Math.max(0, e.end - e.start);
    }

    /** Pause: von Hand eingetragen oder automatisch nach Arbeitszeitgesetz (über 6 h: 30, über 9 h: 45 Min.). */
    static long pauseMs(Prefs.Entry e) {
        if (e.isFreeDay()) return 0;
        long g = grossMs(e);
        if (e.pause >= 0) return Math.min(g, e.pause * MIN);
        return g - Fmt.netMs(g);
    }

    /** Netto-Arbeitszeit = Anwesenheit − Pause. */
    static long netMs(Prefs.Entry e) {
        return grossMs(e) - pauseMs(e);
    }

    static LocalDate day(long ms) {
        return Fmt.zoned(ms).toLocalDate();
    }

    // =====================================================================
    // Tagesgrenze über mehrere Einsätze
    // =====================================================================

    /** Ergebnis der Tagesberechnung für einen (neuen oder laufenden) Einsatz. */
    static final class DayInfo {
        /** Wie viele frühere Einsätze zum selben Arbeitstag gehören. */
        int earlierSessions;
        /** Netto-Arbeitszeit der früheren Einsätze. */
        long earlierNet;
        /** Pausen innerhalb der früheren Einsätze. */
        long earlierPause;
        /** Lücken zwischen den Einsätzen (zählen als Pause). */
        long gaps;
        /** Wie viel Netto-Arbeit in diesem Einsatz noch erlaubt ist. */
        long remainingNet;
        /** Pause, die in diesem Einsatz noch dazugerechnet wird. */
        long extraBreak;
        /** Späteste Ausstempelzeit dieses Einsatzes. */
        long deadline;
    }

    /**
     * Berechnet die späteste Ausstempelzeit für einen Einsatz, der um {@code start} beginnt.
     *
     * Frühere Einsätze zählen zum selben Arbeitstag, solange dazwischen weniger als 11 Stunden
     * Ruhezeit lagen und sie innerhalb der letzten 24 Stunden begonnen haben.
     *
     *  - Kein früherer Einsatz:  Start + Höchstarbeitszeit + Pause      (z. B. + 10 h + 45 Min.)
     *  - Mit früheren Einsätzen: Start + (Höchstarbeitszeit − schon gearbeitet)
     *                                  + (Pause − schon gemachte Pausen − Lücken, mind. 0)
     *
     * @param hist nach Beginn sortierter Verlauf (ohne den laufenden Einsatz)
     */
    static DayInfo dayInfo(List<Prefs.Entry> hist, long start, int maxWorkMin, int breakMin) {
        DayInfo d = new DayInfo();
        long next = start;
        for (int i = hist.size() - 1; i >= 0; i--) {
            Prefs.Entry e = hist.get(i);
            if (e.isFreeDay() || e.start >= start) continue;
            long end = Math.min(e.end, next);
            long gap = Math.max(0, next - end);
            if (gap >= REST_NEW_DAY) break;          // genug Ruhezeit -> neuer Arbeitstag
            if (start - e.start >= 24 * HOUR) break; // mehr als 24 h her
            if (gap >= MIN_BREAK_BLOCK) d.gaps += gap;
            d.earlierNet += netMs(e);
            d.earlierPause += pauseMs(e);
            d.earlierSessions++;
            next = e.start;
        }
        long max = maxWorkMin * MIN;
        long brk = breakMin * MIN;
        if (d.earlierSessions == 0) {
            d.remainingNet = max;
            d.extraBreak = brk;
        } else {
            d.remainingNet = Math.max(0, max - d.earlierNet);
            d.extraBreak = Math.max(0, brk - d.earlierPause - d.gaps);
        }
        d.deadline = start + d.remainingNet + d.extraBreak;
        return d;
    }

    // =====================================================================
    // Überstunden
    // =====================================================================

    /** Ein Kalendertag mit allen seinen Einträgen. */
    static final class Day {
        final LocalDate date;
        /** Positionen der Einträge im Verlauf (für Bearbeiten). */
        final List<Integer> indexes = new ArrayList<>();
        long net;
        long pause;
        long soll;
        long diff;

        Day(LocalDate date) {
            this.date = date;
        }
    }

    /**
     * Fasst den Verlauf zu Tagen zusammen (nach dem Datum des Beginns).
     * An jedem Tag mit mindestens einem Eintrag gilt die Sollzeit, auch an einem
     * „freien Tag“ (Überstundenabbau), der dann 0 Ist-Stunden hat.
     * Tage ohne Eintrag (frei, Urlaub, krank) zählen weder Soll noch Ist.
     */
    static List<Day> days(List<Prefs.Entry> hist, int sollMin) {
        List<Day> out = new ArrayList<>();
        Day cur = null;
        for (int i = 0; i < hist.size(); i++) {
            Prefs.Entry e = hist.get(i);
            LocalDate d = day(e.start);
            if (cur == null || !cur.date.equals(d)) {
                cur = new Day(d);
                cur.soll = sollMin * MIN;
                out.add(cur);
            }
            cur.indexes.add(i);
            cur.net += netMs(e);
            cur.pause += pauseMs(e);
        }
        for (Day d : out) d.diff = d.net - d.soll;
        return out;
    }

    /** Nur die Tage eines Monats. */
    static List<Day> daysInMonth(List<Day> all, YearMonth ym) {
        List<Day> out = new ArrayList<>();
        for (Day d : all) {
            if (YearMonth.from(d.date).equals(ym)) out.add(d);
        }
        return out;
    }

    /** Überstundenkonto = Übertrag + Summe aller Tagesdifferenzen + Buchungen (z. B. Auszahlungen). */
    static long balanceMs(List<Day> days, int startBalanceMin, List<Prefs.Adjustment> adjustments) {
        long sum = startBalanceMin * MIN;
        for (Day d : days) sum += d.diff;
        for (Prefs.Adjustment a : adjustments) sum += a.minutes * MIN;
        return sum;
    }

    /** Stunden über der Grenze (z. B. 40 h), die ausgezahlt werden. */
    static long payoutMs(long balanceMs, int limitMin) {
        return Math.max(0, balanceMs - limitMin * MIN);
    }

    /** Auszahlungsbetrag brutto in Euro: Stunden × Stundenlohn × (1 + Zuschlag). */
    static double payoutEuro(long payoutMs, double hourlyRate, int surchargePct) {
        double hours = payoutMs / (double) HOUR;
        return hours * hourlyRate * (1 + surchargePct / 100.0);
    }
}
