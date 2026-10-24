package de.marcel.zeitwaechter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

/** Alle Benachrichtigungen der App. */
final class Notifier {

    private Notifier() {
    }

    static final String CH_WARN = "warnungen";
    static final String CH_STATUS = "status";
    static final String CH_INFO = "kommen_gehen";

    static final int ID_STATUS = 1;
    static final int ID_INFO = 2;
    static final int ID_WARN = 3;

    private static final int COLOR = 0xFF1E5AA8;

    static void createChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);

        NotificationChannel warn = new NotificationChannel(CH_WARN,
                "Warnungen vor der Höchstarbeitszeit", NotificationManager.IMPORTANCE_HIGH);
        warn.setDescription("Erinnert dich rechtzeitig ans Ausstempeln.");
        warn.enableVibration(true);
        warn.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 500});

        NotificationChannel status = new NotificationChannel(CH_STATUS,
                "Laufende Arbeitszeit", NotificationManager.IMPORTANCE_LOW);
        status.setDescription("Zeigt den Countdown bis zur spätesten Ausstempelzeit.");

        NotificationChannel info = new NotificationChannel(CH_INFO,
                "Kommen & Gehen", NotificationManager.IMPORTANCE_DEFAULT);
        info.setDescription("Meldet, wenn der Arbeitsort erreicht oder verlassen wurde.");

        nm.createNotificationChannel(warn);
        nm.createNotificationChannel(status);
        nm.createNotificationChannel(info);
    }

    private static PendingIntent openApp(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, 1, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Notification.Action action(Context c, String action, String title, int requestCode) {
        Intent i = new Intent(c, ActionReceiver.class);
        i.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(c, requestCode, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_stat_clock), title, pi).build();
    }

    /** Knopf, der die App öffnet und sofort die Uhrzeitauswahl für die Startzeit zeigt. */
    private static Notification.Action editStartAction(Context c, int requestCode) {
        Intent i = new Intent(c, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_EDIT_START, true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, requestCode, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_stat_clock), "Startzeit anpassen", pi).build();
    }

    private static NotificationManager nm(Context c) {
        createChannels(c);
        return c.getSystemService(NotificationManager.class);
    }

    /** Dauerhafte Anzeige mit Countdown, solange eingestempelt. */
    static void showStatus(Context c) {
        if (!Prefs.active(c)) {
            cancelStatus(c);
            return;
        }
        long start = Prefs.start(c);
        long dl = SessionManager.deadline(c, start);

        Notification n = new Notification.Builder(c, CH_STATUS)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setColor(COLOR)
                .setContentTitle("Spätestens ausstempeln: " + Fmt.timeSmart(dl))
                .setContentText("Eingestempelt seit " + Fmt.time(start) + " (geschätzt)")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setWhen(dl)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setContentIntent(openApp(c))
                .addAction(editStartAction(c, 20))
                .addAction(action(c, ActionReceiver.ACTION_END, "Ausgestempelt", 10))
                .addAction(action(c, ActionReceiver.ACTION_DISCARD, "Kein Arbeitstag", 11))
                .build();
        nm(c).notify(ID_STATUS, n);
    }

    static void cancelStatus(Context c) {
        c.getSystemService(NotificationManager.class).cancel(ID_STATUS);
    }

    /** Meldung beim Ankommen. */
    static void showArrived(Context c, long stampIn, long deadline) {
        Notification n = new Notification.Builder(c, CH_INFO)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setColor(COLOR)
                .setContentTitle("Angekommen – eingestempelt ca. " + Fmt.time(stampIn))
                .setContentText("Spätestens ausstempeln: " + Fmt.timeSmart(deadline))
                .setStyle(new Notification.BigTextStyle().bigText(
                        "Geschätzte Einstempelzeit: " + Fmt.time(stampIn) + "\n"
                                + "Spätestens ausstempeln: " + Fmt.timeSmart(deadline) + "\n"
                                + "Weicht die Stempeluhr ab? Auf „Startzeit anpassen“ tippen."))
                .setAutoCancel(true)
                .setContentIntent(openApp(c))
                .addAction(editStartAction(c, 21))
                .addAction(action(c, ActionReceiver.ACTION_DISCARD, "Kein Arbeitstag", 12))
                .build();
        nm(c).notify(ID_INFO, n);
    }

    /** Allgemeine Info-Meldung. */
    static void showInfo(Context c, String title, String text) {
        Notification n = new Notification.Builder(c, CH_INFO)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setColor(COLOR)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(openApp(c))
                .build();
        nm(c).notify(ID_INFO, n);
    }

    /** Die wichtige Warnung vor der Grenze. */
    static void showWarning(Context c, int minutesLeft, long deadline) {
        String limit = Fmt.hoursFromMinutes(Prefs.maxWorkMin(c)) + "-h-Grenze";
        String title = minutesLeft > 0
                ? "Noch " + minutesLeft + " Min. bis zur " + limit + "!"
                : limit + " erreicht – jetzt ausstempeln!";
        String text = "Spätestens um " + Fmt.timeSmart(deadline) + " ausstempeln (inkl. "
                + Prefs.breakMin(c) + " Min. Pause).";

        Notification n = new Notification.Builder(c, CH_WARN)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setColor(minutesLeft > 10 ? 0xFFE08A00 : 0xFFC62828)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openApp(c))
                .addAction(action(c, ActionReceiver.ACTION_END, "Ausgestempelt", 13))
                .build();
        nm(c).notify(ID_WARN, n);
    }

    static void cancelWarning(Context c) {
        c.getSystemService(NotificationManager.class).cancel(ID_WARN);
    }
}
