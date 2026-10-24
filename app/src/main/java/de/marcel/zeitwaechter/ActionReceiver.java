package de.marcel.zeitwaechter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Knöpfe in den Benachrichtigungen ("Ausgestempelt", "Kein Arbeitstag"). */
public class ActionReceiver extends BroadcastReceiver {

    static final String ACTION_END = "de.marcel.zeitwaechter.END";
    static final String ACTION_DISCARD = "de.marcel.zeitwaechter.DISCARD";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_END.equals(action)) {
            SessionManager.end(context, System.currentTimeMillis(), true);
        } else if (ACTION_DISCARD.equals(action)) {
            SessionManager.discard(context);
            Notifier.showInfo(context, "Verworfen", "Diese Sitzung wurde nicht gespeichert.");
        }
    }
}
