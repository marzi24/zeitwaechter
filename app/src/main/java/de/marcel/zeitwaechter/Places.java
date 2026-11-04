package de.marcel.zeitwaechter;

import android.annotation.SuppressLint;
import android.content.Context;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Ermittelt beim Einstempeln von Hand, wo man gerade ist:
 * im Bereich „Zuhause“ → Homeoffice, im Bereich „Arbeit“ → Büro, sonst unbekannt.
 */
final class Places {

    private Places() {
    }

    /** Entfernung in Metern zwischen zwei Koordinaten (Haversine-Formel). */
    static double distance(double lat1, double lon1, double lat2, double lon2) {
        double r = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    /**
     * Ordnet einen Standort zu. Die Ungenauigkeit des Standorts (accuracy) wird großzügig mitgerechnet,
     * höchstens aber 150 m. Liegt beides in Reichweite, gewinnt der nähere Ort.
     *
     * @param home  {lat, lon, radius} oder null
     * @param work  {lat, lon, radius} oder null
     */
    static String classify(double lat, double lon, float accuracy, double[] home, double[] work) {
        double tol = Math.min(150, Math.max(0, accuracy));
        double dHome = home == null ? Double.MAX_VALUE : distance(lat, lon, home[0], home[1]);
        double dWork = work == null ? Double.MAX_VALUE : distance(lat, lon, work[0], work[1]);
        boolean inHome = home != null && dHome <= home[2] + tol;
        boolean inWork = work != null && dWork <= work[2] + tol;
        if (inHome && inWork) return dHome <= dWork ? Prefs.PLACE_HOME : Prefs.PLACE_OFFICE;
        if (inHome) return Prefs.PLACE_HOME;
        if (inWork) return Prefs.PLACE_OFFICE;
        return Prefs.PLACE_UNKNOWN;
    }

    /** Kurzer Name für Anzeigen. */
    static String label(String place) {
        if (Prefs.PLACE_HOME.equals(place)) return "Homeoffice";
        if (Prefs.PLACE_OFFICE.equals(place)) return "Büro";
        return "Ort unbekannt";
    }

    static String icon(String place) {
        return Prefs.PLACE_HOME.equals(place) ? "🏠" : "🏢";
    }

    /**
     * Für die gerade gestartete Sitzung den Ort per Standort ermitteln.
     * {@code done} wird danach auf dem Hauptthread aufgerufen (auch wenn nichts erkannt wurde).
     */
    @SuppressLint("MissingPermission")
    static void detectForSession(Context context, Runnable done) {
        final Context app = context.getApplicationContext();
        if (!GeofenceHelper.hasFine(app) || (!Prefs.hasHomeLocation(app) && !Prefs.hasWorkLocation(app))) {
            if (done != null) done.run();
            return;
        }
        final long sessionStart = Prefs.start(app);
        try {
            LocationServices.getFusedLocationProviderClient(app)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(loc -> {
                        if (loc != null && Prefs.active(app) && Prefs.start(app) == sessionStart
                                && Prefs.PLACE_UNKNOWN.equals(Prefs.sessionPlace(app))) {
                            double[] home = Prefs.hasHomeLocation(app)
                                    ? new double[]{Prefs.homeLat(app), Prefs.homeLon(app), Prefs.homeRadius(app)} : null;
                            double[] work = Prefs.hasWorkLocation(app)
                                    ? new double[]{Prefs.lat(app), Prefs.lon(app), Prefs.radius(app)} : null;
                            Prefs.setSessionPlace(app, classify(loc.getLatitude(), loc.getLongitude(),
                                    loc.getAccuracy(), home, work));
                            Notifier.showStatus(app);
                        }
                        if (done != null) done.run();
                    })
                    .addOnFailureListener(e -> {
                        if (done != null) done.run();
                    });
        } catch (RuntimeException e) {
            if (done != null) done.run();
        }
    }
}
