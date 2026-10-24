package de.marcel.zeitwaechter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Hilfsfunktionen zum Formatieren von Zeiten. */
final class Fmt {

    private Fmt() {
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EE dd.MM.", Locale.GERMANY);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMANY);

    static ZonedDateTime zoned(long ms) {
        return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault());
    }

    /** "07:05" */
    static String time(long ms) {
        return TIME.format(zoned(ms));
    }

    /** "07:05", oder "07:05 (Sa 25.10.)" wenn es nicht heute ist – wichtig bei Nachtdiensten. */
    static String timeSmart(long ms) {
        ZonedDateTime z = zoned(ms);
        if (z.toLocalDate().equals(LocalDate.now(ZoneId.systemDefault()))) {
            return TIME.format(z);
        }
        return TIME.format(z) + " (" + DAY.format(z) + ")";
    }

    /** "Mo. 21.10." */
    static String day(long ms) {
        return DAY.format(zoned(ms));
    }

    /** "21.10.2026" */
    static String date(long ms) {
        return DATE.format(zoned(ms));
    }

    /** "21.10.2026 07:05" */
    static String dateTime(long ms) {
        return DATE_TIME.format(zoned(ms));
    }

    /** Dauer als "9:09 h". */
    static String duration(long ms) {
        long min = Math.max(0, ms) / 60_000L;
        return String.format(Locale.GERMANY, "%d:%02d h", min / 60, min % 60);
    }

    /** Dauer als "3 Std. 12 Min." */
    static String durationWords(long ms) {
        long min = Math.max(0, ms) / 60_000L;
        long h = min / 60;
        long m = min % 60;
        if (h == 0) return m + " Min.";
        return h + " Std. " + m + " Min.";
    }

    /** Dauer in Stunden mit Komma für CSV, z. B. "9,15". */
    static String hoursDecimal(long ms) {
        return String.format(Locale.GERMANY, "%.2f", Math.max(0, ms) / 3_600_000.0);
    }

    /** Minuten als Stunden-Text: 600 -> "10", 570 -> "9,5". */
    static String hoursFromMinutes(int min) {
        if (min % 60 == 0) return String.valueOf(min / 60);
        String s = String.format(Locale.GERMANY, "%.2f", min / 60.0);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(",")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * Geschätzte Netto-Arbeitszeit nach Arbeitszeitgesetz (§ 4 ArbZG):
     * über 6 h mind. 30 Min. Pause, über 9 h mind. 45 Min. Pause.
     * Nur eine Schätzung – die tatsächlichen Pausen kennt die App nicht.
     */
    static long netMs(long grossMs) {
        long six = 6 * 3_600_000L;
        long nine = 9 * 3_600_000L;
        long net = grossMs;
        if (net > six) net = Math.max(six, grossMs - 30 * 60_000L);
        if (net > nine) net = Math.max(nine, grossMs - 45 * 60_000L);
        return net;
    }
}
