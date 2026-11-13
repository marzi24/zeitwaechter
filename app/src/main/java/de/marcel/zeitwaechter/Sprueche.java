package de.marcel.zeitwaechter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Motivationssprüche für den Heute-Bildschirm.
 *
 * Je nach Lage (nicht eingestempelt, im Minus, kurz vor dem Soll, im Plus, kurz vor der Grenze …)
 * wird eine passende Liste gewählt. Platzhalter in geschweiften Klammern werden durch echte Zahlen
 * ersetzt:
 *   {rest}   = was bis zum Soll noch fehlt        {plus}  = wie viel du schon im Plus bist
 *   {grenze} = Zeit bis zur Grenze                {um}    = Uhrzeit, ab der du im Plus bist
 *   {soll}   = Sollzeit heute
 * Der Spruch wechselt alle 20 Minuten von selbst, beim Antippen sofort.
 */
final class Sprueche {

    private Sprueche() {
    }

    /** Die verschiedenen Lagen, für die es eigene Sprüche gibt. */
    enum Lage {
        NOCH_NICHT_DA, FREIER_TAG, START, MINUS, KNAPP, PLUS, FREI_GEARBEITET,
        GRENZE_NAH, GRENZE_UEBER, FEIERABEND_PLUS, FEIERABEND_MINUS
    }

    /** Ein Spruch: Emoji links, Text rechts. */
    static final class Spruch {
        final String emoji;
        final String text;

        Spruch(String emoji, String text) {
            this.emoji = emoji;
            this.text = text;
        }

        @Override
        public String toString() {
            return emoji + " " + text;
        }
    }

    private static final String[][] NOCH_NICHT_DA = {
            {"☕", "Noch nicht eingestempelt – der Kaffee in der Arbeit wird auch nicht wärmer."},
            {"🌅", "Neuer Tag, neue Minuten. Heute stehen {soll} h auf dem Plan."},
            {"🚗", "Die Stempeluhr vermisst dich schon ein bisschen."},
            {"😴", "Noch im Ruhemodus. Völlig okay – die Arbeit läuft nicht weg."},
            {"📋", "{soll} h Soll heute. Klingt machbar, oder?"},
            {"🦸", "Jeder Held fängt mal mit Einstempeln an."},
    };

    private static final String[][] FREIER_TAG = {
            {"🏖️", "Heute keine Sollzeit – genieß den freien Tag!"},
            {"🛋️", "Couch statt Stempeluhr. Sehr vernünftige Entscheidung."},
            {"🔋", "Akku aufladen ist heute die wichtigste Aufgabe."},
            {"🌞", "Die Arbeit kann warten. Heute bist du dran."},
            {"🍖", "Freier Tag! Perfekt für einen Ausflug, den Grill oder einfach gar nichts."},
    };

    private static final String[][] START = {
            {"🚀", "Los geht's! Noch {rest} bis zum Soll."},
            {"☕", "Eingestempelt. Erstmal Kaffee – der zählt ja schon mit."},
            {"💪", "Guter Start! Der Tag gehört dir."},
            {"🎬", "Und … Action! Ab {um} Uhr geht's ins Plus."},
            {"🧭", "Kurs gesetzt: Soll erreicht um {um} Uhr."},
    };

    private static final String[][] MINUS = {
            {"🐢", "Noch {rest} bis zum Soll. Schritt für Schritt."},
            {"⛰️", "Der Gipfel ist um {um} Uhr erreicht. Weiter so!"},
            {"🧱", "Stein auf Stein – noch {rest}, dann steht das Soll."},
            {"🎧", "Musik an, Fokus rein. Noch {rest}."},
            {"🏃", "Das Minus ist nur ein Zwischenstand."},
            {"⏱️", "Jede Minute zählt – die Stempeluhr rechnet ganz genau mit."},
            {"🍽️", "Pause nicht vergessen – ab 6 h wird sie sowieso abgezogen."},
    };

    private static final String[][] KNAPP = {
            {"🏁", "Endspurt! Nur noch {rest} bis zum Soll."},
            {"⏳", "Um {um} Uhr bist du im Plus. Fast geschafft!"},
            {"🔥", "Noch {rest} – das schaffst du mit links."},
            {"🎯", "Das Soll ist in Sichtweite."},
            {"🥨", "Gleich geschafft. Zeit, schon mal an den Feierabend zu denken."},
    };

    private static final String[][] PLUS = {
            {"🎉", "Plus! Du hast deinem Arbeitgeber heute einen richtig guten Dienst erwiesen."},
            {"💰", "{plus} im Plus – das Überstundenkonto sagt Danke."},
            {"🏆", "Soll erfüllt! Alles ab jetzt ist Bonus."},
            {"😎", "Ab jetzt arbeitest du sozusagen mit Zuschlag."},
            {"🦸", "Heldenmodus aktiv: {plus} über dem Soll."},
            {"🚪", "Soll geschafft. Du darfst übrigens auch einfach gehen."},
            {"🍰", "Das Soll ist im Sack – gönn dir nachher was."},
    };

    private static final String[][] FREI_GEARBEITET = {
            {"💪", "Heute ohne Soll – jede Minute ist Plus. Bisher {plus}."},
            {"🏅", "Arbeit am freien Tag – dafür gibt's immerhin Zuschlag."},
            {"🫡", "Respekt, dass du heute ran musst. Schon {plus} gesammelt."},
            {"🌇", "Heute zählt jede Stunde doppelt gut – naja, zumindest mit Zuschlag."},
    };

    private static final String[][] GRENZE_NAH = {
            {"🧳", "Noch {grenze} bis zur Grenze. Langsam zusammenpacken!"},
            {"🏠", "Der Feierabend ruft – in {grenze} ist Schluss."},
            {"⚠️", "Genug Heldentum für heute – in {grenze} ist die Grenze erreicht."},
            {"🧥", "Jacke schon mal griffbereit legen."},
            {"🔔", "Die Grenze kommt näher – bloß nicht verpassen."},
    };

    private static final String[][] GRENZE_UEBER = {
            {"🚨", "Grenze erreicht – jetzt aber wirklich ausstempeln!"},
            {"🏃", "Lauf, Forrest, lauf … zur Stempeluhr!"},
            {"⛔", "Mehr geht heute nicht. Ab nach Hause!"},
    };

    private static final String[][] FEIERABEND_PLUS = {
            {"🍻", "Feierabend mit {plus} Plus. Stark!"},
            {"🛋️", "Feierabend! Heute hast du mehr als geliefert."},
            {"🌙", "Gut gemacht – jetzt Füße hoch."},
            {"🎉", "Plus! Dein Arbeitgeber kann sich heute nicht beschweren."},
    };

    private static final String[][] FEIERABEND_MINUS = {
            {"🌱", "Feierabend! {rest} fehlen – das holst du locker wieder rein."},
            {"🧘", "Kürzerer Tag? Gönn dir. Morgen ist auch noch ein Tag."},
            {"🙂", "Work-Life-Balance: heute mit Schwerpunkt Life."},
            {"📉", "Kleines Minus, großes Lächeln."},
    };

    /** Zusätzliche Sprüche fürs Homeoffice (kommen beim Arbeiten dazu). */
    private static final String[][] HOMEOFFICE = {
            {"🏠", "Homeoffice: kurzer Arbeitsweg, lange Konzentration."},
            {"🩳", "Homeoffice – obenrum Business, untenrum Jogginghose?"},
            {"☕", "Der beste Kaffee der Firma: dein eigener."},
    };

    /** Welche Lage gerade herrscht. */
    static Lage lage(Work.Today t, boolean active) {
        if (active) {
            if (t.left <= 0) return Lage.GRENZE_UEBER;
            if (t.left <= 60 * Work.MIN) return Lage.GRENZE_NAH;
            if (t.soll <= 0) return Lage.FREI_GEARBEITET;
            if (t.plus()) return Lage.PLUS;
            if (t.dayNet < 30 * Work.MIN) return Lage.START;
            if (-t.diff <= 60 * Work.MIN) return Lage.KNAPP;
            return Lage.MINUS;
        }
        if (t.hasWork) return t.plus() ? Lage.FEIERABEND_PLUS : Lage.FEIERABEND_MINUS;
        return t.soll > 0 ? Lage.NOCH_NICHT_DA : Lage.FREIER_TAG;
    }

    /**
     * Sucht einen passenden Spruch aus.
     *
     * @param home  läuft gerade ein Homeoffice-Einsatz?
     * @param now   jetzt (Millisekunden)
     * @param tippe wie oft auf den Spruch getippt wurde (schaltet weiter)
     */
    static Spruch pick(Work.Today t, boolean active, boolean home, long now, int tippe) {
        Lage lage = lage(t, active);
        LocalDateTime jetzt = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault());

        List<String[]> liste = new ArrayList<>();
        for (String[] s : tabelle(lage)) liste.add(s);

        boolean arbeitet = lage == Lage.START || lage == Lage.MINUS || lage == Lage.KNAPP || lage == Lage.PLUS;
        if (arbeitet && home) {
            for (String[] s : HOMEOFFICE) liste.add(s);
        }
        if (arbeitet || lage == Lage.NOCH_NICHT_DA) {
            DayOfWeek wt = jetzt.getDayOfWeek();
            if (wt == DayOfWeek.MONDAY) liste.add(new String[]{"☕", "Montag. Wir schaffen das – mit viel Kaffee."});
            if (wt == DayOfWeek.FRIDAY) liste.add(new String[]{"🍺", "Freitag! Das Wochenende ist schon in Sichtweite."});
        }
        if (lage == Lage.START && jetzt.getHour() < 7) {
            liste.add(new String[]{"🐦", "Der frühe Vogel stempelt ein."});
        }

        // Sprüche mit {um} nur, wenn es eine Plus-Uhrzeit gibt
        if (t.plusAt <= 0) liste.removeIf(s -> s[1].contains("{um}"));
        if (liste.isEmpty()) return null;

        // Wechselt alle 20 Minuten und beim Antippen; Startpunkt hängt vom Datum ab
        long block = now / (20 * Work.MIN);
        long tag = jetzt.toLocalDate().toEpochDay();
        long idx = block + tag * 7 + lage.ordinal() * 3L + tippe;
        String[] s = liste.get((int) Math.floorMod(idx, (long) liste.size()));
        return new Spruch(s[0], fuellen(s[1], t));
    }

    private static String[][] tabelle(Lage l) {
        switch (l) {
            case NOCH_NICHT_DA:
                return NOCH_NICHT_DA;
            case FREIER_TAG:
                return FREIER_TAG;
            case START:
                return START;
            case MINUS:
                return MINUS;
            case KNAPP:
                return KNAPP;
            case PLUS:
                return PLUS;
            case FREI_GEARBEITET:
                return FREI_GEARBEITET;
            case GRENZE_NAH:
                return GRENZE_NAH;
            case GRENZE_UEBER:
                return GRENZE_UEBER;
            case FEIERABEND_PLUS:
                return FEIERABEND_PLUS;
            default:
                return FEIERABEND_MINUS;
        }
    }

    /** Ersetzt die Platzhalter durch echte Werte. */
    static String fuellen(String text, Work.Today t) {
        return text
                .replace("{rest}", dauer(-t.diff))
                .replace("{plus}", dauer(t.diff))
                .replace("{grenze}", dauer(t.left))
                .replace("{um}", t.plusAt > 0 ? Fmt.time(t.plusAt) : "")
                .replace("{soll}", Fmt.hm(t.soll))
                .replace("Min..", "Min."); // "… noch 30 Min.." → ein Punkt reicht
    }

    /** "45 Min." oder "1:23 h". */
    static String dauer(long ms) {
        long min = Math.max(0, ms) / Work.MIN;
        if (min < 60) return min + " Min.";
        return String.format(Locale.GERMANY, "%d:%02d h", min / 60, min % 60);
    }
}
