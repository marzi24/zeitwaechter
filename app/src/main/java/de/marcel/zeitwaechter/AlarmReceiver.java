package de.marcel.zeitwaechter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Wird zu den geplanten Warnzeiten aufgerufen und zeigt die Warnung an. */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long sessionStart = intent.getLongExtra(SessionManager.EXTRA_SESSION_START, -1);
        int minutes = intent.getIntExtra(SessionManager.EXTRA_MINUTES, 0);

        // Nur warnen, wenn die Sitzung noch dieselbe ist (nicht schon ausgestempelt/korrigiert).
        if (!Prefs.active(context) || Prefs.start(context) != sessionStart) return;

        Notifier.showWarning(context, minutes, SessionManager.deadline(context, sessionStart));
    }
}
