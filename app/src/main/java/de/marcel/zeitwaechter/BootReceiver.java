package de.marcel.zeitwaechter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Nach einem Neustart, einem App-Update oder wenn der Standort wieder eingeschaltet wird,
 * löscht Android Geofences bzw. Wecker. Hier wird alles wieder eingerichtet.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofenceHelper.register(context);
        if (Prefs.active(context)) {
            SessionManager.scheduleAlarms(context);
            Notifier.showStatus(context);
        }
    }
}
