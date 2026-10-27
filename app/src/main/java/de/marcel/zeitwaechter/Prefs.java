package de.marcel.zeitwaechter;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Speichert alles lokal auf dem Handy: Einstellungen, die laufende Sitzung und den Verlauf.
 * Es wird nichts ins Internet geschickt.
 */
public final class Prefs {

    private Prefs() {
    }

    private static final String FILE = "zeitwaechter";

    // Einstellungen
    private static final String K_LAT = "work_lat";
    private static final String K_LON = "work_lon";
    private static final String K_RADIUS = "radius_m";
    private static final String K_OFFSET = "offset_min";
    private static final String K_OFFSET_OUT = "offset_out_min";
    private static final String K_MAX_WORK = "max_work_min";
    private static final String K_BREAK = "break_min";
    private static final String K_WARN = "warn_list";
    private static final String K_RESUME = "resume_window_min";
    private static final String K_ENABLED = "tracking_enabled";

    // Laufende Sitzung
    private static final String K_ACTIVE = "session_active";
    private static final String K_START = "session_start";
    private static final String K_SOURCE = "session_source";
    private static final String K_LAST_END = "last_end";
    private static final String K_LAST_END_MANUAL = "last_end_manual";

    // Sonstiges
    private static final String K_HISTORY = "history";
    private static final String K_GEOFENCE_STATUS = "geofence_status";

    // Überstunden
    private static final String K_SOLL = "soll_min_day";
    private static final String K_PAUSE_AFTER = "pause_after_min";
    private static final String K_LATEST_OUT = "latest_out_min";
    private static final String K_START_BAL = "start_balance_min";
    private static final String K_OT_LIMIT = "ot_limit_min";
    private static final String K_RATE = "hourly_rate";
    private static final String K_ADJ = "ot_adjustments";
    private static final String K_START_DATE = "ot_start_epochday";
    private static final String K_SUR_WEEK = "sur_week";
    private static final String K_SUR_SAT_LOW = "sur_sat_low";
    private static final String K_SAT_LOW_MIN = "sat_low_min";
    private static final String K_SUR_SAT_HIGH = "sur_sat_high";
    private static final String K_SUR_SUN = "sur_sun";
    private static final String K_SUR_HOL = "sur_hol";
    private static final String K_STATE = "holiday_state";
    private static final String K_EXTRA_HOL = "extra_holidays";

    private static final int MAX_HISTORY = 5000;

    /** Quelle für einen freien Tag, an dem Überstunden abgebaut werden. */
    public static final String SOURCE_FREE = "frei";

    /** Ein Eintrag im Verlauf (Zeiten in Millisekunden seit 1970). */
    public static final class Entry {
        public final long start;
        public final long end;
        public final String source;
        /** Pause in Minuten, -1 = automatisch nach Arbeitszeitgesetz. */
        public final int pause;

        public Entry(long start, long end, String source) {
            this(start, end, source, -1);
        }

        public Entry(long start, long end, String source, int pause) {
            this.start = start;
            this.end = end;
            this.source = source;
            this.pause = pause;
        }

        public boolean isFreeDay() {
            return SOURCE_FREE.equals(source);
        }
    }

    /** Eine Buchung auf dem Überstundenkonto, z. B. eine Auszahlung (minutes negativ). */
    public static final class Adjustment {
        public final long time;
        public final int minutes;
        public final String note;

        public Adjustment(long time, int minutes, String note) {
            this.time = time;
            this.minutes = minutes;
            this.note = note;
        }
    }

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ---------------- Arbeitsort ----------------

    public static boolean hasWorkLocation(Context c) {
        return sp(c).contains(K_LAT) && sp(c).contains(K_LON);
    }

    public static double lat(Context c) {
        return Double.parseDouble(sp(c).getString(K_LAT, "0"));
    }

    public static double lon(Context c) {
        return Double.parseDouble(sp(c).getString(K_LON, "0"));
    }

    public static void setWorkLocation(Context c, double lat, double lon) {
        sp(c).edit()
                .putString(K_LAT, Double.toString(lat))
                .putString(K_LON, Double.toString(lon))
                .commit();
    }

    public static void clearWorkLocation(Context c) {
        sp(c).edit().remove(K_LAT).remove(K_LON).commit();
    }

    // ---------------- Einstellungen ----------------

    /** Radius um den Arbeitsort in Metern. */
    public static int radius(Context c) {
        return sp(c).getInt(K_RADIUS, 150);
    }

    /** Minuten vom Erreichen des Ortes bis zum Einstempeln (Parken + Laufen). */
    public static int offsetMin(Context c) {
        return sp(c).getInt(K_OFFSET, 2);
    }

    /** Minuten vom Ausstempeln bis zum Verlassen des Arbeitsorts (Laufen zum Auto + Losfahren). */
    public static int offsetOutMin(Context c) {
        return sp(c).getInt(K_OFFSET_OUT, offsetMin(c));
    }

    /** Späteste Ausstempel-Uhrzeit in Minuten ab Mitternacht (Standard 20:45), -1 = keine. */
    public static int latestOutMin(Context c) {
        return sp(c).getInt(K_LATEST_OUT, 20 * 60 + 45);
    }

    public static void saveLatestOut(Context c, int minuteOfDay) {
        sp(c).edit().putInt(K_LATEST_OUT, minuteOfDay).commit();
    }

    public static void saveOffsetOut(Context c, int minutes) {
        sp(c).edit().putInt(K_OFFSET_OUT, minutes).commit();
    }

    /** Maximale Arbeitszeit in Minuten (ohne Pause), Standard 10 h. */
    public static int maxWorkMin(Context c) {
        return sp(c).getInt(K_MAX_WORK, 600);
    }

    /** Pause in Minuten, die zusätzlich zur Arbeitszeit eingerechnet wird. */
    public static int breakMin(Context c) {
        return sp(c).getInt(K_BREAK, 45);
    }

    /** Wird man innerhalb dieser Minuten wieder erkannt, läuft die alte Sitzung weiter. */
    public static int resumeMin(Context c) {
        return sp(c).getInt(K_RESUME, 45);
    }

    /** Warnzeiten als Text, z. B. "30, 15, 5". */
    public static String warnList(Context c) {
        return sp(c).getString(K_WARN, "30, 15, 5");
    }

    public static boolean trackingEnabled(Context c) {
        return sp(c).getBoolean(K_ENABLED, true);
    }

    public static void saveSettings(Context c, int radius, int offset, int maxWork, int brk,
                                    int resume, String warn, boolean enabled) {
        sp(c).edit()
                .putInt(K_RADIUS, radius)
                .putInt(K_OFFSET, offset)
                .putInt(K_MAX_WORK, maxWork)
                .putInt(K_BREAK, brk)
                .putInt(K_RESUME, resume)
                .putString(K_WARN, warn)
                .putBoolean(K_ENABLED, enabled)
                .commit();
    }

    /** Wandelt "30, 15, 5" in die Liste [30, 15, 5] um. Ungültiges wird ignoriert. */
    public static List<Integer> warnMinutes(Context c) {
        return parseWarnList(warnList(c));
    }

    public static List<Integer> parseWarnList(String text) {
        List<Integer> out = new ArrayList<>();
        for (String part : text.split("[,; ]+")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            try {
                int m = Integer.parseInt(p);
                if (m > 0 && m <= 600 && !out.contains(m)) out.add(m);
            } catch (NumberFormatException ignored) {
                // überspringen
            }
        }
        return out;
    }

    // ---------------- Laufende Sitzung ----------------

    public static boolean active(Context c) {
        return sp(c).getBoolean(K_ACTIVE, false);
    }

    /** Geschätzter Einstempel-Zeitpunkt der laufenden Sitzung. */
    public static long start(Context c) {
        return sp(c).getLong(K_START, 0);
    }

    public static String source(Context c) {
        return sp(c).getString(K_SOURCE, "");
    }

    public static void setSession(Context c, long start, String source) {
        sp(c).edit()
                .putBoolean(K_ACTIVE, true)
                .putLong(K_START, start)
                .putString(K_SOURCE, source)
                .commit();
    }

    public static void clearSession(Context c) {
        sp(c).edit()
                .putBoolean(K_ACTIVE, false)
                .remove(K_START)
                .remove(K_SOURCE)
                .commit();
    }

    public static long lastEnd(Context c) {
        return sp(c).getLong(K_LAST_END, 0);
    }

    public static boolean lastEndManual(Context c) {
        return sp(c).getBoolean(K_LAST_END_MANUAL, false);
    }

    public static void setLastEnd(Context c, long time, boolean manual) {
        sp(c).edit()
                .putLong(K_LAST_END, time)
                .putBoolean(K_LAST_END_MANUAL, manual)
                .commit();
    }

    // ---------------- Verlauf ----------------

    /** Alle Einträge, nach Beginn sortiert (ältester zuerst). */
    public static List<Entry> history(Context c) {
        List<Entry> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp(c).getString(K_HISTORY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Entry(o.getLong("s"), o.getLong("e"), o.optString("q", ""), o.optInt("p", -1)));
            }
        } catch (JSONException ignored) {
            // beschädigter Verlauf -> leer anzeigen
        }
        list.sort((a, b) -> Long.compare(a.start, b.start));
        return list;
    }

    private static void saveHistory(Context c, List<Entry> list) {
        List<Entry> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> Long.compare(a.start, b.start));
        JSONArray arr = new JSONArray();
        int from = Math.max(0, sorted.size() - MAX_HISTORY);
        try {
            for (int i = from; i < sorted.size(); i++) {
                Entry e = sorted.get(i);
                JSONObject o = new JSONObject();
                o.put("s", e.start);
                o.put("e", e.end);
                o.put("q", e.source);
                o.put("p", e.pause);
                arr.put(o);
            }
        } catch (JSONException ignored) {
            // kann bei long/String/int nicht passieren
        }
        sp(c).edit().putString(K_HISTORY, arr.toString()).commit();
    }

    /** Ganze Liste speichern (z. B. nach Bearbeiten in der Tabelle). Wird automatisch sortiert. */
    public static void setHistory(Context c, List<Entry> list) {
        saveHistory(c, list);
    }

    public static void addHistory(Context c, Entry e) {
        List<Entry> list = history(c);
        list.add(e);
        saveHistory(c, list);
    }

    /**
     * Entfernt den zuletzt beendeten Arbeits-Eintrag (keinen freien Tag) und gibt ihn zurück (oder null).
     * Wird gebraucht, wenn man nach kurzer Abwesenheit zurückkommt und die Sitzung weiterläuft.
     */
    public static Entry popLastHistory(Context c) {
        List<Entry> list = history(c);
        int best = -1;
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            if (e.isFreeDay()) continue;
            if (best < 0 || e.end > list.get(best).end) best = i;
        }
        if (best < 0) return null;
        Entry last = list.remove(best);
        saveHistory(c, list);
        return last;
    }

    /** Ersetzt den letzten Verlaufseintrag (zum nachträglichen Korrigieren). */
    public static void replaceLastHistory(Context c, Entry e) {
        List<Entry> list = history(c);
        if (list.isEmpty()) return;
        list.set(list.size() - 1, e);
        saveHistory(c, list);
    }

    public static void clearHistory(Context c) {
        sp(c).edit().remove(K_HISTORY).commit();
    }

    // ---------------- Überstunden ----------------

    /** Sollarbeitszeit pro Arbeitstag in Minuten (Standard 8 h). */
    public static int sollMin(Context c) {
        return sp(c).getInt(K_SOLL, 480);
    }

    /** Übertrag / Startsaldo des Überstundenkontos in Minuten (kann negativ sein). */
    public static int startBalanceMin(Context c) {
        return sp(c).getInt(K_START_BAL, 0);
    }

    /** Ab diesem Kontostand wird ausgezahlt (Standard 40 h). */
    public static int otLimitMin(Context c) {
        return sp(c).getInt(K_OT_LIMIT, 2400);
    }

    /** Stundenlohn brutto in Euro. */
    public static double hourlyRate(Context c) {
        try {
            return Double.parseDouble(sp(c).getString(K_RATE, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Stichtag, ab dem das Überstundenkonto rechnet (der Startsaldo gilt zu Beginn dieses Tages).
     * Beim ersten Aufruf wird der 1. des aktuellen Monats festgelegt.
     */
    public static LocalDate otStartDate(Context c) {
        long epochDay = sp(c).getLong(K_START_DATE, Long.MIN_VALUE);
        if (epochDay == Long.MIN_VALUE) {
            LocalDate d = LocalDate.now().withDayOfMonth(1);
            sp(c).edit().putLong(K_START_DATE, d.toEpochDay()).commit();
            return d;
        }
        return LocalDate.ofEpochDay(epochDay);
    }

    public static void saveOvertimeSettings(Context c, int sollMin, int pauseAfterMin, int startBalMin,
                                            LocalDate startDate, int limitMin, double rate) {
        sp(c).edit()
                .putInt(K_SOLL, sollMin)
                .putInt(K_PAUSE_AFTER, pauseAfterMin)
                .putInt(K_START_BAL, startBalMin)
                .putLong(K_START_DATE, startDate.toEpochDay())
                .putInt(K_OT_LIMIT, limitMin)
                .putString(K_RATE, Double.toString(rate))
                .commit();
    }

    /** Ab dieser Anwesenheit (Minuten) wird Pause abgezogen (Standard 6 h). */
    public static int pauseAfterMin(Context c) {
        return sp(c).getInt(K_PAUSE_AFTER, 360);
    }

    /** Alle Rechenregeln: Sollzeit, Pause, Zuschläge und Feiertage. */
    public static Work.Rules rules(Context c) {
        Work.Rules s = new Work.Rules();
        SharedPreferences p = sp(c);
        s.sollMin = p.getInt(K_SOLL, 480);
        s.pauseAfterMin = p.getInt(K_PAUSE_AFTER, 360);
        s.pauseMin = p.getInt(K_BREAK, 45);
        s.latestOutMin = p.getInt(K_LATEST_OUT, 20 * 60 + 45);
        s.week = p.getInt(K_SUR_WEEK, 25);
        s.satLow = p.getInt(K_SUR_SAT_LOW, 25);
        s.satLowMin = p.getInt(K_SAT_LOW_MIN, 120);
        s.satHigh = p.getInt(K_SUR_SAT_HIGH, 50);
        s.sun = p.getInt(K_SUR_SUN, 70);
        s.hol = p.getInt(K_SUR_HOL, 100);
        s.state = p.getString(K_STATE, "BW");
        s.extraHolidays = p.getString(K_EXTRA_HOL, "");
        return s;
    }

    public static void saveRules(Context c, Work.Rules s) {
        sp(c).edit()
                .putInt(K_SUR_WEEK, s.week)
                .putInt(K_SUR_SAT_LOW, s.satLow)
                .putInt(K_SAT_LOW_MIN, s.satLowMin)
                .putInt(K_SUR_SAT_HIGH, s.satHigh)
                .putInt(K_SUR_SUN, s.sun)
                .putInt(K_SUR_HOL, s.hol)
                .putString(K_STATE, s.state)
                .putString(K_EXTRA_HOL, s.extraHolidays)
                .commit();
    }

    public static List<Adjustment> adjustments(Context c) {
        List<Adjustment> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp(c).getString(K_ADJ, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Adjustment(o.getLong("t"), o.optInt("m", 0), o.optString("n", "")));
            }
        } catch (JSONException ignored) {
            // beschädigt -> leer
        }
        return list;
    }

    private static void saveAdjustments(Context c, List<Adjustment> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Adjustment a : list) {
                JSONObject o = new JSONObject();
                o.put("t", a.time);
                o.put("m", a.minutes);
                o.put("n", a.note);
                arr.put(o);
            }
        } catch (JSONException ignored) {
            // kann nicht passieren
        }
        sp(c).edit().putString(K_ADJ, arr.toString()).commit();
    }

    public static void addAdjustment(Context c, Adjustment a) {
        List<Adjustment> list = adjustments(c);
        list.add(a);
        saveAdjustments(c, list);
    }

    public static void removeLastAdjustment(Context c) {
        List<Adjustment> list = adjustments(c);
        if (list.isEmpty()) return;
        list.remove(list.size() - 1);
        saveAdjustments(c, list);
    }

    // ---------------- Status der Ortserkennung ----------------

    public static String geofenceStatus(Context c) {
        return sp(c).getString(K_GEOFENCE_STATUS, "Noch nicht eingerichtet.");
    }

    public static void setGeofenceStatus(Context c, String text) {
        sp(c).edit().putString(K_GEOFENCE_STATUS, text).commit();
    }
}
