package de.marcel.zeitwaechter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/** Wird von Android aufgerufen, wenn du den Arbeitsort betrittst oder verlässt. */
public class GeofenceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null) return;
        if (event.hasError()) {
            Prefs.setGeofenceStatus(context, "Fehler bei der Erkennung (Code " + event.getErrorCode()
                    + "). Öffne die App einmal, um sie neu einzurichten.");
            return;
        }

        long now = System.currentTimeMillis();
        long when = now;

        // Die Meldung kann ein paar Minuten verspätet ankommen. Der Zeitstempel des Standorts
        // sagt, wann du wirklich an der Grenze warst – den nehmen wir, wenn er plausibel ist.
        Location loc = event.getTriggeringLocation();
        if (loc != null) {
            long t = loc.getTime();
            if (t > 0 && t <= now && now - t < 30 * 60_000L) when = t;
        }

        int transition = event.getGeofenceTransition();
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            SessionManager.onArrive(context, when);
        } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            SessionManager.onLeave(context, when);
        }
    }
}
