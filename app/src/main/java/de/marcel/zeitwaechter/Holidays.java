package de.marcel.zeitwaechter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Gesetzliche Feiertage in Deutschland je Bundesland, inklusive der beweglichen Feiertage
 * (Ostern, Himmelfahrt, Pfingsten, Fronleichnam, Buß- und Bettag).
 * Dazu kommen frei wählbare Zusatztage wie „24.12, 31.12“.
 * Reiner Java-Code ohne Android, damit er testbar ist.
 */
final class Holidays {

    private Holidays() {
    }

    /** Kürzel und Namen der Bundesländer (Reihenfolge = Auswahlliste). */
    static final String[] STATE_CODES = {
            "BW", "BY", "BE", "BB", "HB", "HH", "HE", "MV", "NI", "NW", "RP", "SL", "SN", "ST", "SH", "TH"};
    static final String[] STATE_NAMES = {
            "Baden-Württemberg", "Bayern", "Berlin", "Brandenburg", "Bremen", "Hamburg", "Hessen",
            "Mecklenburg-Vorpommern", "Niedersachsen", "Nordrhein-Westfalen", "Rheinland-Pfalz", "Saarland",
            "Sachsen", "Sachsen-Anhalt", "Schleswig-Holstein", "Thüringen"};

    static int stateIndex(String code) {
        for (int i = 0; i < STATE_CODES.length; i++) if (STATE_CODES[i].equals(code)) return i;
        return 0;
    }

    /** Ostersonntag nach der Gaußschen Osterformel (gregorianischer Kalender). */
    static LocalDate easter(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    private static boolean in(String state, String... states) {
        return Arrays.asList(states).contains(state);
    }

    /** Alle Feiertage eines Jahres (Datum → Name), sortiert. */
    static Map<LocalDate, String> forYear(int year, String state, String extra) {
        Map<LocalDate, String> m = new TreeMap<>();
        LocalDate e = easter(year);

        // Bundesweit
        m.put(LocalDate.of(year, 1, 1), "Neujahr");
        m.put(e.minusDays(2), "Karfreitag");
        m.put(e.plusDays(1), "Ostermontag");
        m.put(LocalDate.of(year, 5, 1), "Tag der Arbeit");
        m.put(e.plusDays(39), "Christi Himmelfahrt");
        m.put(e.plusDays(50), "Pfingstmontag");
        m.put(LocalDate.of(year, 10, 3), "Tag der Deutschen Einheit");
        m.put(LocalDate.of(year, 12, 25), "1. Weihnachtstag");
        m.put(LocalDate.of(year, 12, 26), "2. Weihnachtstag");

        // Je Bundesland
        if (in(state, "BW", "BY", "ST")) m.put(LocalDate.of(year, 1, 6), "Heilige Drei Könige");
        if (in(state, "BE") && year >= 2019) m.put(LocalDate.of(year, 3, 8), "Frauentag");
        if (in(state, "MV") && year >= 2023) m.put(LocalDate.of(year, 3, 8), "Frauentag");
        if (in(state, "BB")) {
            m.put(e, "Ostersonntag");
            m.put(e.plusDays(49), "Pfingstsonntag");
        }
        if (in(state, "BW", "BY", "HE", "NW", "RP", "SL")) m.put(e.plusDays(60), "Fronleichnam");
        if (in(state, "SL")) m.put(LocalDate.of(year, 8, 15), "Mariä Himmelfahrt");
        if (in(state, "TH") && year >= 2019) m.put(LocalDate.of(year, 9, 20), "Weltkindertag");
        if (in(state, "BB", "MV", "SN", "ST", "TH")
                || (in(state, "HB", "HH", "NI", "SH") && year >= 2018)) {
            m.put(LocalDate.of(year, 10, 31), "Reformationstag");
        }
        if (in(state, "BW", "BY", "NW", "RP", "SL")) m.put(LocalDate.of(year, 11, 1), "Allerheiligen");
        if (in(state, "SN")) {
            // Mittwoch vor dem 23. November
            LocalDate d = LocalDate.of(year, 11, 22);
            while (d.getDayOfWeek() != DayOfWeek.WEDNESDAY) d = d.minusDays(1);
            m.put(d, "Buß- und Bettag");
        }

        // Zusätzliche, selbst eingetragene Tage
        for (MonthDay md : parseExtra(extra)) {
            if (md.isValidYear(year)) {
                LocalDate d = md.atYear(year);
                if (!m.containsKey(d)) m.put(d, "Zusatz-Feiertag");
            }
        }
        return m;
    }

    /** "24.12, 31.12." → [24.12., 31.12.]; Ungültiges wird ignoriert. */
    static List<MonthDay> parseExtra(String extra) {
        List<MonthDay> out = new ArrayList<>();
        if (extra == null) return out;
        for (String part : extra.split("[,;\\s]+")) {
            String p = part.trim();
            if (p.endsWith(".")) p = p.substring(0, p.length() - 1);
            if (p.isEmpty()) continue;
            String[] dm = p.split("\\.");
            if (dm.length != 2) continue;
            try {
                int day = Integer.parseInt(dm[0].trim());
                int month = Integer.parseInt(dm[1].trim());
                out.add(MonthDay.of(month, day));
            } catch (RuntimeException ignored) {
                // ungültig -> überspringen
            }
        }
        return out;
    }

    /** Prüft, ob der Zusatz-Text nur gültige Einträge enthält. */
    static boolean extraValid(String extra) {
        if (extra == null || extra.trim().isEmpty()) return true;
        int parts = 0;
        for (String p : extra.split("[,;\\s]+")) if (!p.trim().isEmpty()) parts++;
        return parseExtra(extra).size() == parts;
    }

    /** Name des Feiertags oder null. */
    static String name(LocalDate d, String state, String extra) {
        return forYear(d.getYear(), state, extra).get(d);
    }

    static boolean isHoliday(LocalDate d, String state, String extra) {
        return name(d, state, extra) != null;
    }
}
