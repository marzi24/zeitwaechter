package de.marcel.zeitwaechter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Richtet den "Geofence" ein: einen unsichtbaren Kreis um den Arbeitsort.
 * Android meldet sich dann bei der App, sobald du den Kreis betrittst oder verlässt –
 * auch wenn die App geschlossen ist.
 */
final class GeofenceHelper {

    private GeofenceHelper() {
    }

    private static final String GEOFENCE_ID = "arbeit";

    static boolean hasFine(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasBackground(Context c) {
        if (Build.VERSION.SDK_INT < 29) return hasFine(c);
        return c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static PendingIntent pendingIntent(Context c) {
        Intent i = new Intent(c, GeofenceReceiver.class);
        // MUTABLE ist hier Pflicht: Google trägt die Ereignis-Daten in den Intent ein.
        return PendingIntent.getBroadcast(c, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    @SuppressLint("MissingPermission")
    static void register(Context context) {
        final Context app = context.getApplicationContext();

        if (!Prefs.trackingEnabled(app)) {
            unregister(app);
            Prefs.setGeofenceStatus(app, "Automatische Erkennung ist ausgeschaltet.");
            return;
        }
        if (!Prefs.hasWorkLocation(app)) {
            Prefs.setGeofenceStatus(app, "Noch kein Arbeitsort gespeichert.");
            return;
        }
        if (!hasFine(app) || !hasBackground(app)) {
            Prefs.setGeofenceStatus(app, "Standort-Berechtigung fehlt („Immer zulassen“).");
            return;
        }

        Geofence fence = new Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(Prefs.lat(app), Prefs.lon(app), Prefs.radius(app))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(0) // beim Einrichten nicht sofort auslösen
                .addGeofence(fence)
                .build();

        LocationServices.getGeofencingClient(app)
                .addGeofences(request, pendingIntent(app))
                .addOnSuccessListener(v -> Prefs.setGeofenceStatus(app,
                        "Aktiv (Radius " + Prefs.radius(app) + " m) – eingerichtet "
                                + Fmt.dateTime(System.currentTimeMillis())))
                .addOnFailureListener(e -> Prefs.setGeofenceStatus(app, "Fehler: " + describe(e)));
    }

    static void unregister(Context context) {
        Context app = context.getApplicationContext();
        LocationServices.getGeofencingClient(app).removeGeofences(pendingIntent(app));
    }

    static String describe(Exception e) {
        if (e instanceof ApiException) {
            int code = ((ApiException) e).getStatusCode();
            switch (code) {
                case 1000:
                    return "Standort ist ausgeschaltet oder „Immer zulassen“ fehlt (Code 1000).";
                case 1001:
                    return "Zu viele Geofences (Code 1001).";
                case 1002:
                    return "Zu viele Anfragen (Code 1002).";
                case 1004:
                    return "Standort-Berechtigung nicht ausreichend (Code 1004).";
                default:
                    return "Code " + code;
            }
        }
        return String.valueOf(e.getMessage());
    }
}
