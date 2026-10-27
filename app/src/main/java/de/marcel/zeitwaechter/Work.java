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

    /**
     * Automatische Pause nach der Regel des Arbeitgebers:
     * Ab {@code pauseAfterMin} Anwesenheit wird nur so viel abgezogen, dass man genau auf diese Zeit kommt,
     * höchstens {@code pauseMin}. Standard: ab 6 h, max. 45 Min.
     *   5:50 h → 0 Min.   6:32 h → 32 Min. (= 6:00 netto)   6:45 h und mehr → 45 Min.
     */
    static long pauseForGross(long grossMs, Rules r) {
        long over = grossMs - r.pauseAfterMin * MIN;
        if (over <= 0) return 0;
        return Math.min(over, r.pauseMin * MIN);
    }

    /** Netto-Zeit für eine Anwesenheit mit automatischer Pause. */
    static long netForGross(long grossMs, Rules r) {
        return Math.max(0, grossMs) - pauseForGross(Math.max(0, grossMs), r);
    }

    /** Pause: von Hand eingetragen oder automatisch (siehe {@link #pauseForGross}). */
    static long pauseMs(Prefs.Entry e, Rules r) {
        if (e.isFreeDay()) return 0;
        long g = grossMs(e);
        if (e.pause >= 0) return Math.min(g, e.pause * MIN);
        return pauseForGross(g, r);
    }

    /** Netto-Arbeitszeit = Anwesenheit − Pause. */
    static long netMs(Prefs.Entry e, Rules r) {
        return grossMs(e) - pauseMs(e, r);
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
        /** Späteste Ausstempelzeit dieses Einsatzes (die frühere der beiden Grenzen). */
        long deadline;
        /** Grenze nur nach Höchstarbeitszeit (z. B. 10 h + Pause). */
        long workDeadline;
        /** Feste Uhrzeit-Grenze (z. B. 20:45) oder 0, wenn ausgeschaltet. */
        long clockDeadline;
        /** true, wenn die Uhrzeit-Grenze früher greift als die 10-h-Grenze. */
        boolean byClock;
    }

    /**
     * Nächster Zeitpunkt ab {@code start}, an dem die Uhr {@code minuteOfDay} zeigt.
     * Beginnt man vor 20:45, ist es 20:45 am selben Tag; beginnt man danach (Nachtdienst),
     * ist es 20:45 am nächsten Tag – dann bremst diese Grenze praktisch nicht.
     */
    static long nextClockTime(long start, int minuteOfDay) {
        java.time.ZonedDateTime z = Fmt.zoned(start);
        java.time.ZonedDateTime cap = z.toLocalDate().atTime(minuteOfDay / 60, minuteOfDay % 60)
                .atZone(z.getZone());
        if (!cap.toInstant().isAfter(z.toInstant())) cap = cap.plusDays(1);
        return cap.toInstant().toEpochMilli();
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
    static DayInfo dayInfo(List<Prefs.Entry> hist, long start, int maxWorkMin, Rules r) {
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
            d.earlierNet += netMs(e, r);
            d.earlierPause += pauseMs(e, r);
            d.earlierSessions++;
            next = e.start;
        }
        long max = maxWorkMin * MIN;
        long brk = r.pauseMin * MIN;
        if (d.earlierSessions == 0) {
            d.remainingNet = max;
            d.extraBreak = brk;
        } else {
            d.remainingNet = Math.max(0, max - d.earlierNet);
            d.extraBreak = Math.max(0, brk - d.earlierPause - d.gaps);
        }
        d.workDeadline = start + d.remainingNet + d.extraBreak;
        d.deadline = d.workDeadline;
        if (r.latestOutMin >= 0) {
            d.clockDeadline = nextClockTime(start, r.latestOutMin);
            if (d.clockDeadline < d.workDeadline) {
                d.deadline = d.clockDeadline;
                d.byClock = true;
            }
        }
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
     * Sollzeit eines Tages: nur Montag–Freitag, nicht an Feiertagen.
     * Samstag, Sonntag und Feiertage haben 0 – dort ist jede Stunde eine Überstunde.
     */
    static long sollFor(LocalDate d, Rules r) {
        return type(d, r.state, r.extraHolidays) == DayType.WERKTAG ? r.sollMin * MIN : 0;
    }

    /**
     * Fasst den Verlauf zu Tagen zusammen (nach dem Datum des Beginns).
     * An Werktagen (Mo–Fr, kein Feiertag) mit mindestens einem Eintrag gilt die Sollzeit,
     * auch an einem „freien Tag“ (Überstundenabbau), der dann 0 Ist-Stunden hat.
     * Tage ohne Eintrag (frei, Urlaub, krank) zählen weder Soll noch Ist.
     */
    static List<Day> days(List<Prefs.Entry> hist, Rules r) {
        List<Day> out = new ArrayList<>();
        Day cur = null;
        for (int i = 0; i < hist.size(); i++) {
            Prefs.Entry e = hist.get(i);
            LocalDate d = day(e.start);
            if (cur == null || !cur.date.equals(d)) {
                cur = new Day(d);
                cur.soll = sollFor(d, r);
                out.add(cur);
            }
            cur.indexes.add(i);
            cur.net += netMs(e, r);
            cur.pause += pauseMs(e, r);
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

    // =====================================================================
    // Tagesart und Zuschläge
    // =====================================================================

    enum DayType { WERKTAG, SAMSTAG, SONNTAG, FEIERTAG }

    /** Feiertag geht vor Sonntag, Sonntag vor Samstag. */
    static DayType type(LocalDate d, String state, String extraHolidays) {
        if (Holidays.isHoliday(d, state, extraHolidays)) return DayType.FEIERTAG;
        switch (d.getDayOfWeek()) {
            case SUNDAY:
                return DayType.SONNTAG;
            case SATURDAY:
                return DayType.SAMSTAG;
            default:
                return DayType.WERKTAG;
        }
    }

    /** Alle Rechenregeln aus den Einstellungen: Sollzeit, Pause, Zuschläge, Feiertage. */
    static final class Rules {
        int sollMin = 480;      // Sollzeit pro Werktag Mo–Fr
        int pauseAfterMin = 360; // Pause wird ab 6 h Anwesenheit abgezogen …
        int pauseMin = 45;      // … höchstens 45 Minuten
        int latestOutMin = -1;  // späteste Ausstempel-Uhrzeit in Minuten ab Mitternacht (20:45 = 1245), -1 = aus
        int week = 25;          // Montag–Freitag
        int satLow = 25;        // Samstag, die ersten satLowMin Minuten Überstunden
        int satLowMin = 120;    // = 2 Stunden
        int satHigh = 50;       // Samstag ab der 3. Stunde
        int sun = 70;           // Sonntag
        int hol = 100;          // Feiertag
        String state = "BW";
        String extraHolidays = "";
    }

    /** Überstunden eines Zeitraums, aufgeteilt nach Zuschlagsstufe. */
    static final class OtSplit {
        long week, satLow, satHigh, sun, hol;

        long total() {
            return week + satLow + satHigh + sun + hol;
        }

        /** Zuschlag in „Prozent-Stunden“, z. B. 2 h × 25 % = 50. */
        double pctHours(Rules s) {
            return (week * s.week + satLow * s.satLow + satHigh * s.satHigh + sun * s.sun + hol * s.hol)
                    / (double) HOUR;
        }

        /** Durchschnittlicher Zuschlag aller Überstunden in Prozent. */
        double averagePct(Rules s) {
            long t = total();
            if (t <= 0) return s.week;
            return pctHours(s) / (t / (double) HOUR);
        }
    }

    /**
     * Teilt die Überstunden (nur Tage mit Plus) nach Tagesart auf.
     * Samstag: die ersten {@code satLowMin} Minuten mit satLow, der Rest mit satHigh.
     */
    static OtSplit split(List<Day> days, Rules s) {
        OtSplit o = new OtSplit();
        for (Day d : days) {
            if (d.diff <= 0) continue;
            switch (type(d.date, s.state, s.extraHolidays)) {
                case FEIERTAG:
                    o.hol += d.diff;
                    break;
                case SONNTAG:
                    o.sun += d.diff;
                    break;
                case SAMSTAG:
                    long low = Math.min(d.diff, s.satLowMin * MIN);
                    o.satLow += low;
                    o.satHigh += d.diff - low;
                    break;
                default:
                    o.week += d.diff;
            }
        }
        return o;
    }

    // =====================================================================
    // Monatskonto: am Monatsende wird alles über der Grenze ausgezahlt
    // =====================================================================

    /** Abrechnung eines Monats. */
    static final class Month {
        YearMonth ym;
        /** Erster gezählter Tag (bei Start-Monat das Startdatum, sonst der 1.). */
        LocalDate from;
        long startBal;
        long diff;
        long endBal;
        /** Stunden über der Grenze, die ausgezahlt werden. */
        long payout;
        /** Kontostand, mit dem der nächste Monat beginnt. */
        long carry;
        OtSplit ot;
        List<Day> days;
    }

    /**
     * Rechnet das Überstundenkonto Monat für Monat ab dem Startdatum durch.
     * Kontostand Monatsende = Monatsanfang + Überstunden des Monats.
     * Liegt er über der Grenze, wird der Rest ausgezahlt und der nächste Monat beginnt mit der Grenze.
     * Liegt er darunter, wird er so übernommen.
     *
     * @return Liste der Monate von startDate bis einschließlich {@code until}; leer, wenn until davor liegt.
     */
    static List<Month> account(List<Day> allDays, LocalDate startDate, long startBalMs, long limitMs,
                               YearMonth until, Rules s) {
        List<Month> out = new ArrayList<>();
        YearMonth ym = YearMonth.from(startDate);
        long bal = startBalMs;
        while (!ym.isAfter(until)) {
            Month m = new Month();
            m.ym = ym;
            m.from = ym.equals(YearMonth.from(startDate)) ? startDate : ym.atDay(1);
            m.days = new ArrayList<>();
            for (Day d : allDays) {
                if (YearMonth.from(d.date).equals(ym) && !d.date.isBefore(m.from)) m.days.add(d);
            }
            m.startBal = bal;
            for (Day d : m.days) m.diff += d.diff;
            m.endBal = m.startBal + m.diff;
            m.payout = Math.max(0, m.endBal - limitMs);
            m.carry = m.endBal - m.payout;
            m.ot = split(m.days, s);
            out.add(m);
            bal = m.carry;
            ym = ym.plusMonths(1);
        }
        return out;
    }

    /**
     * Auszahlung brutto in Euro: Stunden × Stundenlohn × (1 + Ø-Zuschlag).
     * Der Zuschlag ist der Durchschnitt über die Überstunden dieses Monats
     * (jede ausgezahlte Stunde wird anteilig nach Werktag/Samstag/Sonntag/Feiertag bewertet).
     */
    static double payoutEuro(Month m, double hourlyRate, Rules s) {
        double hours = m.payout / (double) HOUR;
        return hours * hourlyRate * (1 + m.ot.averagePct(s) / 100.0);
    }
}
