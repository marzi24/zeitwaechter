package de.marcel.zeitwaechter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/**
 * Die eigentliche Logik: Sitzung starten/beenden, späteste Ausstempelzeit berechnen,
 * Erinnerungen planen.
 *
 * Späteste Ausstempelzeit = geschätzte Einstempelzeit + max. Arbeitszeit + Pause
 * Standard: Einstempeln + 10 h + 45 Min. = Einstempeln + 10:45 h
 */
public final class SessionManager {

    private SessionManager() {
    }

    static final String EXTRA_MINUTES = "minutes_left";
    static final String EXTRA_SESSION_START = "session_start";

    private static final int ALARM_BASE = 100;
    private static final int MAX_ALARMS = 12;

    /**
     * Späteste Ausstempelzeit für einen Einstempel-Zeitpunkt.
     * Frühere Einsätze am selben Arbeitstag werden angerechnet (siehe {@link Work#dayInfo}).
     */
    public static long deadline(Context c, long start) {
        return dayInfo(c, start).deadline;
    }

    /** Tagesberechnung (frühere Einsätze, Restzeit, Pause) für einen Einsatz ab {@code start}. */
    public static Work.DayInfo dayInfo(Context c, long start) {
        return Work.dayInfo(Prefs.history(c), start, Prefs.maxWorkMin(c), Prefs.rules(c));
    }

    /** Nach Änderungen am Verlauf: Warnungen und Anzeige der laufenden Sitzung neu berechnen. */
    public static void historyChanged(Context c) {
        if (Prefs.active(c)) {
            scheduleAlarms(c);
            Notifier.showStatus(c);
        }
    }

    // ---------------- Ereignisse von der Ortserkennung ----------------

    /** Arbeitsort erreicht. whenMs = Zeitpunkt, an dem das Handy den Bereich betreten hat. */
    public static void onArrive(Context c, long whenMs) {
        if (!Prefs.trackingEnabled(c)) return;
        if (Prefs.active(c)) return; // läuft schon -> nichts ändern (Startzeit NIE nach hinten schieben)

        long lastEnd = Prefs.lastEnd(c);
        long window = Prefs.resumeMin(c) * 60_000L;
        boolean recentlyEnded = lastEnd > 0 && whenMs >= lastEnd - 10 * 60_000L && whenMs - lastEnd < window;

        if (recentlyEnded && Prefs.lastEndManual(c)) {
            // Du hast kurz vorher selbst "Ausgestempelt"/"Verwerfen" gedrückt.
            // Ein GPS-Sprung soll das nicht rückgängig machen.
            return;
        }

        if (recentlyEnded) {
            // Kurz weg gewesen oder GPS-Sprung: alte Sitzung mit ursprünglicher Startzeit fortsetzen.
            Prefs.Entry last = Prefs.popLastHistory(c);
            if (last != null) {
                Prefs.setSession(c, last.start, last.source);
                Prefs.setLastEnd(c, 0, false);
                scheduleAlarms(c);
                Notifier.showStatus(c);
                Notifier.showInfo(c, "Arbeitsort wieder erreicht",
                        "Sitzung seit " + Fmt.time(last.start) + " läuft weiter. Spätestens ausstempeln: "
                                + Fmt.timeSmart(deadline(c, last.start)) + ".");
                return;
            }
        }

        long stampIn = whenMs + Prefs.offsetMin(c) * 60_000L;
        start(c, stampIn, "automatisch");
        Notifier.showArrived(c, stampIn, deadline(c, stampIn));
    }

    /** Arbeitsort verlassen. */
    public static void onLeave(Context c, long whenMs) {
        if (!Prefs.active(c)) return;
        long stampOut = whenMs - Prefs.offsetOutMin(c) * 60_000L;
        long start = Prefs.start(c);
        if (stampOut < start) stampOut = start;
        end(c, stampOut, false);
        Notifier.showInfo(c, "Feierabend erkannt",
                "Ausgestempelt ca. " + Fmt.time(stampOut) + " · anwesend " + Fmt.duration(stampOut - start)
                        + " (netto ca. " + Fmt.duration(Work.netForGross(stampOut - start, Prefs.rules(c))) + ")");
    }

    // ---------------- Aktionen ----------------

    /** Neue Sitzung mit gegebener Einstempelzeit starten. */
    public static void start(Context c, long stampIn, String source) {
        Prefs.setSession(c, stampIn, source);
        Prefs.setLastEnd(c, 0, false);
        scheduleAlarms(c);
        Notifier.showStatus(c);
    }

    /** Startzeit der laufenden Sitzung korrigieren (oder Sitzung mit dieser Zeit starten). */
    public static void setStart(Context c, long newStart) {
        if (Prefs.active(c)) {
            Prefs.setSession(c, newStart, "korrigiert");
            scheduleAlarms(c);
            Notifier.showStatus(c);
        } else {
            start(c, newStart, "manuell");
        }
    }

    /** Sitzung beenden und im Verlauf speichern. manual = vom Nutzer ausgelöst. */
    public static void end(Context c, long stampOut, boolean manual) {
        if (!Prefs.active(c)) return;
        long start = Prefs.start(c);
        if (stampOut < start) stampOut = start;
        Prefs.addHistory(c, new Prefs.Entry(start, stampOut, Prefs.source(c)));
        Prefs.clearSession(c);
        Prefs.setLastEnd(c, manual ? System.currentTimeMillis() : stampOut, manual);
        cancelAlarms(c);
        Notifier.cancelStatus(c);
        Notifier.cancelWarning(c);
    }

    /** Sitzung verwerfen (z. B. nur am Arbeitsort vorbeigefahren). Kein Verlaufseintrag. */
    public static void discard(Context c) {
        Prefs.clearSession(c);
        Prefs.setLastEnd(c, System.currentTimeMillis(), true);
        cancelAlarms(c);
        Notifier.cancelStatus(c);
        Notifier.cancelWarning(c);
    }

    // ---------------- Erinnerungen ----------------

    /** Plant alle Warnungen (z. B. 30, 15, 5 Min. vorher und genau zur Grenze). */
    public static void scheduleAlarms(Context c) {
        cancelAlarms(c);
        if (!Prefs.active(c)) return;

        long start = Prefs.start(c);
        long dl = deadline(c, start);
        long now = System.currentTimeMillis();

        List<Integer> minutes = new ArrayList<>(Prefs.warnMinutes(c));
        if (!minutes.contains(0)) minutes.add(0); // Alarm genau zur Grenze

        AlarmManager am = c.getSystemService(AlarmManager.class);
        int idx = 0;
        for (int m : minutes) {
            if (idx >= MAX_ALARMS) break;
            long at = dl - m * 60_000L;
            if (at <= now) continue;
            Intent i = new Intent(c, AlarmReceiver.class);
            i.putExtra(EXTRA_MINUTES, m);
            i.putExtra(EXTRA_SESSION_START, start);
            PendingIntent pi = PendingIntent.getBroadcast(c, ALARM_BASE + idx, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            setAlarm(c, am, at, pi);
            idx++;
        }
    }

    public static void cancelAlarms(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        for (int idx = 0; idx < MAX_ALARMS; idx++) {
            PendingIntent pi = PendingIntent.getBroadcast(c, ALARM_BASE + idx,
                    new Intent(c, AlarmReceiver.class),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }

    /** Darf die App minutengenaue Wecker stellen? */
    public static boolean canExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        return c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    private static void setAlarm(Context c, AlarmManager am, long at, PendingIntent pi) {
        try {
            if (canExact(c)) {
                // "Wecker"-Alarm: wird auch im Energiesparmodus pünktlich ausgelöst.
                PendingIntent show = PendingIntent.getActivity(c, 0,
                        new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
