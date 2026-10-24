package de.marcel.zeitwaechter;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private static final int MAX_HISTORY = 400;

    /** Ein Eintrag im Verlauf (Zeiten in Millisekunden seit 1970). */
    public static final class Entry {
        public final long start;
        public final long end;
        public final String source;

        public Entry(long start, long end, String source) {
            this.start = start;
            this.end = end;
            this.source = source;
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

    public static List<Entry> history(Context c) {
        List<Entry> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp(c).getString(K_HISTORY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Entry(o.getLong("s"), o.getLong("e"), o.optString("q", "")));
            }
        } catch (JSONException ignored) {
            // beschädigter Verlauf -> leer anzeigen
        }
        return list;
    }

    private static void saveHistory(Context c, List<Entry> list) {
        JSONArray arr = new JSONArray();
        int from = Math.max(0, list.size() - MAX_HISTORY);
        try {
            for (int i = from; i < list.size(); i++) {
                Entry e = list.get(i);
                JSONObject o = new JSONObject();
                o.put("s", e.start);
                o.put("e", e.end);
                o.put("q", e.source);
                arr.put(o);
            }
        } catch (JSONException ignored) {
            // kann bei long/String nicht passieren
        }
        sp(c).edit().putString(K_HISTORY, arr.toString()).commit();
    }

    public static void addHistory(Context c, Entry e) {
        List<Entry> list = history(c);
        list.add(e);
        saveHistory(c, list);
    }

    /** Entfernt den letzten Verlaufseintrag und gibt ihn zurück (oder null). */
    public static Entry popLastHistory(Context c) {
        List<Entry> list = history(c);
        if (list.isEmpty()) return null;
        Entry last = list.remove(list.size() - 1);
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

    // ---------------- Status der Ortserkennung ----------------

    public static String geofenceStatus(Context c) {
        return sp(c).getString(K_GEOFENCE_STATUS, "Noch nicht eingerichtet.");
    }

    public static void setGeofenceStatus(Context c, String text) {
        sp(c).edit().putString(K_GEOFENCE_STATUS, text).commit();
    }
}
