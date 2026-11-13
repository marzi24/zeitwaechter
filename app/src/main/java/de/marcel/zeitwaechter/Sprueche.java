package de.marcel.zeitwaechter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sarkastische Motivationssprüche für den Heute-Bildschirm.
 *
 * So funktioniert die Auswahl:
 * 1. Aus deiner Lage ergibt sich eine Grundliste (z. B. „im Minus“, „im Plus“, „Feierabend“).
 *    Homeoffice-, Wochentags- und Jahreszeiten-Sprüche kommen dort mit hinein.
 * 2. Trifft ein besonderes Szenario zu (Mittag, Bergfest, 2. Einsatz, Sonntag, Monatsende …),
 *    wird etwa jedes zweite Mal ein Spruch aus diesen Szenarien gezeigt.
 * 3. Ist die Grenze nah oder überschritten, gibt es nur noch Grenz-Sprüche.
 *
 * Platzhalter in geschweiften Klammern werden durch echte Werte ersetzt (siehe {@link #fuellen}).
 * Sprüche, deren Platzhalter gerade keinen Sinn ergeben, werden weggelassen.
 */
final class Sprueche {

    private Sprueche() {
    }

    /** Alles, was die Spruchauswahl über den aktuellen Stand wissen muss. */
    static final class Kontext {
        Work.Today t;
        boolean active;
        /** Laufender Einsatz ist Homeoffice. */
        boolean home;
        /** Heute vorher schon im Büro gewesen, jetzt Homeoffice. */
        boolean mixed;
        /** Einsatz wurde automatisch beim Ankommen gestartet. */
        boolean auto;
        long now;
        /** Beginn des laufenden Einsatzes (0 = keiner). */
        long sessionStart;
        int earlierSessions;
        Work.DayType dayType = Work.DayType.WERKTAG;
        /** Morgen ist kein Soll (Wochenende oder Feiertag). */
        boolean tomorrowFree;
        boolean tomorrowHoliday;
        boolean weekKnown;
        long weekDiff;
        /** Diesen Monat über der Auszahlgrenze (z. B. 40 h)? */
        boolean overLimit;
        double payoutEuro;
        int latestOutMin = -1;
        int pauseAfterMin = 360;
        /** Zuschlag Mo–Fr in Prozent. */
        int weekPct = 25;
        ZoneId zone = ZoneId.systemDefault();
    }

    /** Grundlage. */
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

    // =====================================================================
    // Grundlisten
    // =====================================================================

    static final String[][] NOCH_NICHT_DA = {
            {"😴", "Noch nicht eingestempelt. Genieß die letzten Minuten Freiheit."},
            {"☕", "Die Stempeluhr wartet schon. Sie hat ja sonst nichts vor."},
            {"📋", "{soll} h Soll heute. Klingt nach einem Albtraum, ist aber nur ein {wochentag}."},
            {"🌧️", "Neuer Tag, gleiche Stempeluhr."},
            {"🚗", "Du könntest jetzt auch einfach weiterfahren. Nur so als Gedanke."},
            {"🫠", "Rein rechnerisch ist heute schon wieder ein Arbeitstag. Unverschämt."},
            {"🛏️", "Noch nicht eingestempelt. Das Bett hat dich wohl nicht gehen lassen."},
            {"🧾", "Heute im Angebot: {soll} h Lebenszeit gegen Geld tauschen."},
            {"🌫️", "Ein neuer Tag voller Möglichkeiten. Die meisten davon haben mit Arbeit zu tun."},
            {"🚦", "Jede rote Ampel auf dem Weg ist geschenkte Zeit. Leider nicht bezahlte."},
    };

    static final String[][] FREIER_TAG = {
            {"🛋️", "Kein Soll heute. Der Arbeitgeber muss ohne dich zurechtkommen. Er wird es überleben. Leider."},
            {"🔋", "Akku laden. Montag kommt schneller, als dir lieb ist."},
            {"🏖️", "Freier Tag. Denk bloß nicht an die Arbeit … zu spät."},
            {"📵", "Die Firma ruft heute nicht an. Und wenn doch, bist du im Funkloch."},
            {"🍖", "Heute zählt nur eins: nichts."},
            {"📺", "Dein einziger Termin heute: das Sofa."},
            {"🤫", "Pssst. Wenn du nicht an die Arbeit denkst, denkt sie auch nicht an dich."},
            {"🧺", "Freier Tag! Also Wäsche, Einkaufen, Putzen. Erholung ist was für Rentner."},
    };

    static final String[][] START = {
            {"🎬", "Und … Action. Noch {rest}. Wird schon."},
            {"☕", "Erstmal Kaffee. Der zählt immerhin schon als Arbeitszeit."},
            {"🧭", "Feierabend geplant für {um} Uhr. Hahaha. Guter Witz."},
            {"🪦", "Hier ruht deine Motivation. Sie wurde {netto} alt."},
            {"🐌", "Erst {netto} geschafft. Erst. {netto}"},
            {"🥶", "Eingestempelt. Ab jetzt gehört deine Zeit offiziell jemand anderem."},
            {"🧮", "Noch {restmin} Minuten. Klingt in Minuten auch nicht besser, oder?"},
            {"🫖", "Die ersten Minuten sind die schwersten. Die restlichen auch."},
            {"🪫", "Akku: 100 %. Motivation: lädt noch."},
    };

    static final String[][] MINUS = {
            {"⏳", "Noch {rest}. Die Uhr läuft. Rückwärts, gefühlt."},
            {"🧱", "Noch {rest}. Aber wer zählt schon mit? Ach ja: du."},
            {"🎧", "Musik an, Fokus rein, Seele aus. Noch {rest}."},
            {"🏔️", "Das Soll ist um {um} Uhr erreicht. Der Gipfel der Gefühle."},
            {"🫥", "Einfach weiter so tun, als wärst du beschäftigt. Läuft doch."},
            {"📉", "Du bist im Minus. Wie dein Kontostand. Und deine Laune."},
            {"🍽️", "Pause nicht vergessen. Ab 6 h wird sie dir eh abgezogen, ob du willst oder nicht."},
            {"🤡", "Jede Minute zählt. Leider nur einmal."},
            {"🐌", "Die Zeit vergeht wie im Flug. Wie ein Flug mit 6 Stunden Verspätung."},
            {"🪟", "Noch {rest}. Aus dem Fenster schauen zählt übrigens auch als Arbeitszeit."},
            {"🧟", "Ich arbeite, also bin ich. Müde."},
            {"📊", "Tagesfortschritt: {prozent} %. Der Rest ist Leidensweg."},
            {"🕰️", "Wenn du auf die Uhr starrst, geht sie langsamer. Nicht bewiesen, aber gefühlt."},
            {"🗂️", "Noch {rest}. Vielleicht findest du bis dahin den Sinn des Ganzen."},
            {"🔁", "Gleiche Arbeit, gleiche Uhrzeit, gleiches Gefühl. Täglich grüßt das Murmeltier."},
    };

    static final String[][] KNAPP = {
            {"🏁", "Nur noch {rest} bis zum Soll. Halt durch, Champion."},
            {"⏳", "Um {um} Uhr bist du quitt mit dem Arbeitgeber. Für heute."},
            {"🥱", "Fast geschafft. Also fast so wie gestern."},
            {"🎯", "Das Soll in Sichtweite. Wie eine Fata Morgana."},
            {"🧳", "Innerlich hast du schon ausgestempelt. Jetzt muss nur noch der Körper folgen."},
            {"🫨", "Noch {rest} Jetzt bloß kein Telefon mehr abnehmen."},
            {"🙈", "Jetzt nur nicht so aussehen, als hättest du Zeit für neue Aufgaben."},
            {"📺", "{rest} Das ist eine Serienfolge. Oder ein Meeting, das keiner braucht."},
    };

    static final String[][] PLUS = {
            {"🎉", "Plus! Du hast deinem Arbeitgeber heute einen richtig guten Dienst erwiesen. Er wird es nie erfahren."},
            {"💰", "{plus} im Plus. Das Überstundenkonto freut sich. Dein Sofa weint."},
            {"🏆", "Soll erfüllt. Alles ab jetzt ist freiwillige Selbstaufgabe."},
            {"😎", "Ab jetzt mit {zuschlag} % Zuschlag. Davon kaufst du dir dann … einen Kaffee."},
            {"🦸", "Heldenmodus: {plus} über Soll. Applaus gibt's keinen."},
            {"🚪", "Du darfst übrigens gehen. Das ist die Tür da."},
            {"🫡", "Der Chef würde stolz auf dich sein. Würde."},
            {"🪑", "Noch da? Der Stuhl hat sich schon an dich gewöhnt."},
            {"🧛", "Du bleibst freiwillig länger. Ist bei dir alles okay?"},
            {"📈", "Plus {plus}. Die Firma dankt dir. Nicht persönlich, aber im Herzen."},
            {"🦴", "Wer länger bleibt, hat mehr vom Tag. Also vom Arbeitstag."},
            {"🎖️", "Mitarbeiter des Monats wirst du trotzdem nicht."},
            {"🚪", "Der Ausgang ist da, wo der Parkplatz ist. Nur zur Erinnerung."},
    };

    static final String[][] FREI_GEARBEITET = {
            {"🙃", "Arbeit am freien Tag. Weil Freizeit ja auch überbewertet ist."},
            {"💸", "Jede Minute ist Plus. Bisher {plus}. Dein Privatleben hat angerufen, es will dich zurück."},
            {"🏅", "Immerhin Zuschlag. Freizeit wäre trotzdem schöner gewesen."},
            {"🫠", "Freier Tag, sagten sie. Wird entspannt, sagten sie."},
    };

    static final String[][] GRENZE_NAH = {
            {"🧳", "Noch {grenze} bis zur Grenze. Langsam zusammenpacken, bevor es peinlich wird."},
            {"🏠", "In {grenze} ist Schluss. Die Firma steht morgen auch noch, leider."},
            {"⚠️", "Genug Heldentum für heute. Niemand verleiht dir dafür einen Orden."},
            {"🧥", "Jacke anziehen. Jetzt. Ich mein's ernst."},
            {"🔔", "Grenze in {grenze} Der Arbeitsschutz guckt schon böse."},
            {"⏰", "{grenze} Dann fängt das Handy an zu vibrieren. Und zwar richtig."},
            {"🧯", "Gleich brennt's. Nicht wirklich, aber arbeitsrechtlich."},
            {"💾", "Jetzt alles speichern. Und dann dich selbst nach draußen."},
    };

    static final String[][] GRENZE_UEBER = {
            {"🚨", "Grenze überschritten. Glückwunsch, das war's mit dem Feierabend-Ruhm."},
            {"🏃", "Lauf, Forrest, lauf … zur Stempeluhr!"},
            {"⛔", "Mehr geht nicht. Geh. Sofort. Bevor es jemand merkt."},
            {"📞", "Hallo, hier spricht das Arbeitszeitgesetz. Du sollst nach Hause gehen."},
            {"💀", "Du bist offiziell zu lange hier. Selbst die Putzkraft wundert sich."},
            {"🚓", "Wenn das der Betriebsrat sieht …"},
    };

    static final String[][] FEIERABEND_PLUS = {
            {"🍻", "Feierabend mit {plus} Plus. Geschenkt an die Firma. Gern geschehen."},
            {"🛋️", "Mehr geliefert als bestellt. Typisch du."},
            {"🌙", "Füße hoch. Morgen geht der Spaß von vorne los."},
            {"🎁", "Die Überstunden landen auf dem Konto. Dein Leben bleibt auf Pause."},
            {"🛁", "Feierabend. Soll erfüllt, Lebenszeit verschenkt, Konto wächst."},
            {"🍕", "Mehr gearbeitet als nötig. Das Abendessen hast du dir verdient. Den Nachtisch auch."},
    };

    static final String[][] FEIERABEND_MINUS = {
            {"🙂", "Work-Life-Balance: heute mit Schwerpunkt Life."},
            {"🧘", "{rest} Minus. Der Arbeitgeber wird es verkraften. Hoffentlich."},
            {"📉", "Kleines Minus, großes Lächeln."},
            {"🕵️", "Früher gegangen? Ich hab nichts gesehen."},
            {"🫣", "Früh Feierabend. Das Minus holt dich irgendwann ein. Aber nicht heute."},
            {"🎲", "Heute Minus, morgen Plus. Oder auch nicht. Wer weiß das schon."},
    };

    /** Kommen beim Arbeiten im Homeoffice zur Grundliste dazu. */
    static final String[][] HOMEOFFICE = {
            {"🩳", "Homeoffice: obenrum Business, untenrum Jogginghose."},
            {"☕", "Der beste Kaffee der Firma: dein eigener. Den du selbst bezahlst."},
            {"🐈", "Kollegen im Homeoffice: der Kühlschrank und das Bett. Beide wollen dich ablenken."},
            {"🏠", "Arbeitsweg 12 Sekunden. Motivation trotzdem bei null."},
            {"🛋️", "Homeoffice-Vorteil: Niemand sieht, dass du seit Stunden auf dieselbe Seite starrst."},
            {"🔇", "Kamera aus, Mikro aus, Motivation aus."},
            {"🧺", "Homeoffice: zwischen zwei Mails mal eben die Wäsche aufhängen. Effizienz!"},
    };

    static final String[][] HOMEOFFICE_PLUS = {
            {"🛋️", "Homeoffice im Plus. Dein Sofa vermisst dich. Es steht drei Meter neben dir."},
    };

    // =====================================================================
    // Besondere Szenarien
    // =====================================================================

    static final String[][] ANGEKOMMEN = {
            {"📍", "Angekommen. Die App hat dich erwischt."},
            {"🅿️", "Parkplatz gefunden? Glückwunsch, das war der beste Teil des Tages."},
            {"🚶", "Der Weg zur Stempeluhr: dein heutiges Sportprogramm."},
    };

    static final String[][] MITTAG = {
            {"🍝", "Mittagszeit. Kantine oder Brotdose? Beides traurig."},
            {"🥪", "Hunger? Die Pause wird dir sowieso abgezogen. Also iss gefälligst was."},
            {"😋", "Das Highlight des Tages: Essen. Der Rest ist Beiwerk."},
    };

    static final String[][] PAUSE_NAHT = {
            {"✂️", "Gleich 6 h. Ab dann knabbert die Pause an deiner Zeit."},
            {"🍽️", "In {bisPause} wird Pause abgezogen. Ob du sie machst oder nicht."},
            {"🥨", "Pausenabzug naht. Wenigstens ein Brot sollte drin sein."},
    };

    static final String[][] BERGFEST = {
            {"⛰️", "Bergfest! Die Hälfte ist geschafft. Die schlechtere Hälfte kommt noch."},
            {"🌓", "Halbzeit. Jetzt nochmal genauso lang. Schön, oder?"},
            {"🍾", "Halbzeit! Sekt gibt's keinen, aber noch {rest} Arbeit."},
    };

    static final String[][] ZWEITER_EINSATZ = {
            {"🔁", "Zweite Runde? Hast du was vergessen? Dein Privatleben vielleicht?"},
            {"🌙", "Das Comeback des Tages. Niemand hat danach gefragt."},
            {"🧲", "Die Arbeit lässt dich einfach nicht los, was?"},
            {"➕", "{einsatz}. Einsatz heute. Die Zeit von vorhin ist eingerechnet, keine Sorge. Doch, Sorge."},
    };

    static final String[][] SPAET = {
            {"🌃", "Nach 19 Uhr noch am Arbeiten. Du weißt, dass es draußen dunkel ist, oder?"},
            {"🦉", "Die Nachteule arbeitet. Alle anderen schauen Netflix."},
            {"🕯️", "Spätschicht. Romantisches Licht, unromantische Tätigkeit."},
            {"⏳", "Um {latest} Uhr ist Schluss, egal was passiert. Endlich mal eine klare Regel."},
    };

    static final String[][] FRUEH = {
            {"🌌", "Vor 6:30 Uhr eingestempelt. Selbst der Bäcker schaut neidisch."},
            {"☠️", "So früh, dass es eigentlich noch gestern ist."},
            {"🐦", "Der frühe Vogel stempelt ein. Der späte schläft aus. Rate, wer glücklicher ist."},
    };

    static final String[][] SPAET_DRAN = {
            {"🤒", "Nach 10 Uhr und noch nicht da. Krank? Urlaub? Oder einfach ehrlich zu dir selbst?"},
            {"🕵️", "Schon so spät und noch keine Spur von dir. Respekt."},
            {"📭", "Heute wohl ein Tag ohne Stempeluhr. Ich frag nicht weiter nach."},
    };

    static final String[][] MORGEN_FREI = {
            {"🎈", "Morgen ist frei. Heute zählt nur noch das Überleben."},
            {"🧘", "Morgen frei. Noch {rest} bis zur Freiheit."},
    };

    static final String[][] MORGEN_FEIERTAG = {
            {"🎆", "Tag vor dem Feiertag. Alle sind gedanklich schon weg. Du auch."},
    };

    static final String[][] SONNTAGABEND = {
            {"😩", "Sonntagabend. Das Montagsgefühl klopft schon an."},
            {"📅", "Morgen ist Montag. Ich wollte es nur gesagt haben."},
            {"⌛", "Genieß die letzten Stunden Wochenende. Die vergehen schneller als jede Arbeitsstunde."},
    };

    static final String[][] SAMSTAG = {
            {"🛠️", "Samstagsarbeit. Ab der 3. Stunde gibt's 50 %. Ein Trostpreis."},
            {"🌱", "Samstag in der Arbeit. Die Nachbarn mähen Rasen, du arbeitest Aufgaben ab."},
    };

    static final String[][] SONNTAG = {
            {"⛪", "Sonntag arbeiten. 70 % Zuschlag, 100 % Selbstmitleid."},
            {"🫠", "Am Tag des Herrn in der Arbeit. Der Herr hätte dir freigegeben."},
    };

    static final String[][] FEIERTAG = {
            {"🎉", "Feiertag und du arbeitest. Doppelter Lohn, halbe Laune."},
            {"🏳️", "Alle feiern, du arbeitest. 100 % Zuschlag tröstet. Fast."},
    };

    static final String[][] WOCHE_PLUS = {
            {"📆", "Diese Woche schon {woche}. Die Firma schuldet dir einen Kaffee. Mindestens."},
    };

    static final String[][] WOCHE_MINUS = {
            {"📉", "Die Woche ist bisher im Minus. Na ja, die Woche ist ja noch jung. Oder fast vorbei."},
    };

    static final String[][] KONTO_VOLL = {
            {"💶", "Über 40 h auf dem Konto. Ab jetzt wird bezahlt. Lebenszeit gegen Euro. Fairer Deal?"},
            {"🤑", "Diesen Monat schon ca. {auszahlung} Überstundengeld. Davon kaufst du dir … Freizeit? Ach nein."},
    };

    static final String[][] MONATSENDE = {
            {"📅", "Monatsende. Die Überstunden werden ausgezahlt, der Schlaf leider nicht."},
    };

    static final String[][] GEMISCHT = {
            {"🔀", "Erst Büro, jetzt Homeoffice. Die Arbeit folgt dir überall hin."},
            {"🏢", "Du hast das Büro verlassen. Das Büro dich aber nicht."},
    };

    // =====================================================================
    // Auswahl
    // =====================================================================

    /** Welche Grundlage gerade gilt. */
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

    private static String[][] grundliste(Lage l) {
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

    private static void add(List<String[]> list, String[][] src) {
        for (String[] s : src) list.add(s);
    }

    private static void add(List<String[]> list, String emoji, String text) {
        list.add(new String[]{emoji, text});
    }

    /** Grundliste inkl. Homeoffice-, Wochentags- und Jahreszeiten-Sprüchen. */
    static List<String[]> basis(Kontext k) {
        Lage lage = lage(k.t, k.active);
        List<String[]> l = new ArrayList<>();
        add(l, grundliste(lage));
        if (lage == Lage.GRENZE_NAH || lage == Lage.GRENZE_UEBER) return l;

        LocalDateTime jetzt = LocalDateTime.ofInstant(Instant.ofEpochMilli(k.now), k.zone);
        DayOfWeek wt = jetzt.getDayOfWeek();
        int monat = jetzt.getMonthValue();
        int stunde = jetzt.getHour();
        boolean arbeitet = lage == Lage.START || lage == Lage.MINUS || lage == Lage.KNAPP || lage == Lage.PLUS;
        boolean werktag = lage != Lage.FREIER_TAG && lage != Lage.FREI_GEARBEITET;

        if (arbeitet && k.home) {
            add(l, HOMEOFFICE);
            if (lage == Lage.PLUS) add(l, HOMEOFFICE_PLUS);
        }
        if (werktag && (arbeitet || lage == Lage.NOCH_NICHT_DA)) {
            if (wt == DayOfWeek.MONDAY) add(l, "☕", "Montag. Mehr muss man dazu nicht sagen.");
            if (wt == DayOfWeek.TUESDAY) add(l, "🙄", "Dienstag. Wie Montag, nur ohne Ausrede.");
            if (wt == DayOfWeek.WEDNESDAY) {
                add(l, "🐪", "Mittwoch. Bergfest der Woche. Ab jetzt geht's bergab. In jeder Hinsicht.");
            }
            if (wt == DayOfWeek.THURSDAY) add(l, "🫣", "Donnerstag. Fast Freitag. Fast ist aber nicht ganz.");
            if (wt == DayOfWeek.FRIDAY) {
                add(l, "🍺", "Freitag! Nur noch ein paar Stunden durchhalten, dann darfst du dich vom Arbeiten erholen.");
            }
            if (wt == DayOfWeek.MONDAY && stunde < 10) {
                add(l, "☕", "Montagmorgen. Kaffee ist keine Option, sondern Pflicht.");
            }
        }
        if (monat == 12 && jetzt.getDayOfMonth() < 24 && lage != Lage.FREIER_TAG) {
            add(l, "🎄", "Bald Weihnachten. Durchhalten, dann darfst du dich bei der Familie erholen. Haha.");
        }
        if ((monat >= 11 || monat <= 2) && (arbeitet || lage == Lage.FEIERABEND_PLUS
                || lage == Lage.FEIERABEND_MINUS) && (stunde < 8 || stunde >= 16)) {
            add(l, "🌑", "Im Dunkeln gekommen, im Dunkeln gehen. Vitamin D wird überschätzt.");
        }
        if (monat >= 6 && monat <= 8 && arbeitet && stunde >= 13 && stunde < 19) {
            add(l, "☀️", "Draußen 30 Grad, drinnen Arbeitszeit. Das Leben ist ungerecht.");
        }
        return l;
    }

    /** Sprüche aus den besonderen Szenarien, die gerade zutreffen. */
    static List<String[]> szenarien(Kontext k) {
        Lage lage = lage(k.t, k.active);
        List<String[]> l = new ArrayList<>();
        if (lage == Lage.GRENZE_NAH || lage == Lage.GRENZE_UEBER) return l;

        Work.Today t = k.t;
        LocalDateTime jetzt = LocalDateTime.ofInstant(Instant.ofEpochMilli(k.now), k.zone);
        DayOfWeek wt = jetzt.getDayOfWeek();
        int minuteDesTages = jetzt.getHour() * 60 + jetzt.getMinute();
        int stunde = jetzt.getHour();
        boolean feierabend = lage == Lage.FEIERABEND_PLUS || lage == Lage.FEIERABEND_MINUS;

        if (k.active) {
            long seit = k.now - k.sessionStart;
            if (k.auto && k.earlierSessions == 0 && seit < 5 * Work.MIN) add(l, ANGEKOMMEN);
            if (minuteDesTages >= 11 * 60 + 30 && minuteDesTages < 13 * 60 + 30) add(l, MITTAG);
            long schwelle = k.pauseAfterMin * Work.MIN;
            if (k.earlierSessions == 0 && t.sessionGross >= schwelle - 30 * Work.MIN && t.sessionGross < schwelle) {
                add(l, PAUSE_NAHT);
            }
            if (t.soll > 0 && Math.abs(t.dayNet - t.soll / 2) <= 15 * Work.MIN) add(l, BERGFEST);
            if (k.earlierSessions > 0) add(l, ZWEITER_EINSATZ);
            if (stunde >= 19) add(l, SPAET);
            if (k.sessionStart > 0) {
                LocalDateTime beginn = LocalDateTime.ofInstant(Instant.ofEpochMilli(k.sessionStart), k.zone);
                int bm = beginn.getHour() * 60 + beginn.getMinute();
                if (bm < 6 * 60 + 30 && seit < 3 * Work.HOUR && k.earlierSessions == 0) add(l, FRUEH);
            }
            if (t.soll > 0 && k.tomorrowFree) {
                add(l, MORGEN_FREI);
                if (k.tomorrowHoliday) add(l, MORGEN_FEIERTAG);
            }
            if (t.soll <= 0) {
                if (k.dayType == Work.DayType.SAMSTAG) add(l, SAMSTAG);
                if (k.dayType == Work.DayType.SONNTAG) add(l, SONNTAG);
                if (k.dayType == Work.DayType.FEIERTAG) add(l, FEIERTAG);
            }
            if (k.home && k.mixed) add(l, GEMISCHT);
            if (wt == DayOfWeek.FRIDAY && stunde >= 14 && t.soll > 0) {
                add(l, "🧨", "Freitagnachmittag in der Arbeit. Alle anderen sind schon weg. Du bist das letzte Licht.");
            }
            if (jetzt.getMonthValue() == 12 && jetzt.getDayOfMonth() == 24) {
                add(l, "🎅", "Heiligabend in der Arbeit. Der Weihnachtsmann hat dich offensichtlich vergessen.");
            }
            if (jetzt.getMonthValue() == 12 && jetzt.getDayOfMonth() == 31) {
                add(l, "🎆", "Silvester in der Arbeit. Guter Vorsatz fürs neue Jahr: das nicht wiederholen.");
            }
        } else {
            if (lage == Lage.NOCH_NICHT_DA && stunde >= 10 && stunde < 18) add(l, SPAET_DRAN);
            if (lage == Lage.FREIER_TAG && wt == DayOfWeek.SUNDAY && stunde >= 17) add(l, SONNTAGABEND);
            if (feierabend && k.weekKnown) add(l, k.weekDiff >= 0 ? WOCHE_PLUS : WOCHE_MINUS);
            if (feierabend && wt == DayOfWeek.FRIDAY) {
                add(l, "🍻", "Freitag, Feierabend, Wochenende. Die drei schönsten Wörter der Welt.");
            }
        }
        if (k.overLimit && lage != Lage.FREIER_TAG) {
            add(l, KONTO_VOLL);
            int tag = jetzt.getDayOfMonth();
            if (tag > jetzt.toLocalDate().lengthOfMonth() - 3) add(l, MONATSENDE);
        }
        return l;
    }

    /** Kann der Spruch mit den aktuellen Werten sinnvoll gefüllt werden? */
    static boolean passt(String text, Kontext k) {
        Work.Today t = k.t;
        boolean minus = t.diff < 0;
        if (!minus && (text.contains("{rest}") || text.contains("{restmin}") || text.contains("{prozent}"))) return false;
        if (minus && text.contains("{plus}")) return false;
        if (text.contains("{plus}") && t.diff < Work.MIN) return false;
        if (text.contains("{um}") && t.plusAt <= 0) return false;
        if (text.contains("{prozent}") && t.soll <= 0) return false;
        if (text.contains("{netto}") && t.dayNet < Work.MIN) return false;
        if (text.contains("{latest}") && k.latestOutMin < 0) return false;
        if (text.contains("{auszahlung}") && k.payoutEuro < 1) return false;
        if (text.contains("{grenze}") && t.left <= 0) return false;
        return true;
    }

    /**
     * Sucht einen Spruch aus.
     *
     * @param tippe wie oft auf den Spruch getippt wurde (schaltet weiter)
     */
    static Spruch pick(Kontext k, int tippe) {
        List<String[]> basis = filter(basis(k), k);
        List<String[]> szen = filter(szenarien(k), k);
        if (basis.isEmpty() && szen.isEmpty()) return null;

        // Wechselt alle 20 Minuten und beim Antippen; Startpunkt hängt vom Datum ab
        LocalDateTime jetzt = LocalDateTime.ofInstant(Instant.ofEpochMilli(k.now), k.zone);
        long block = k.now / (20 * Work.MIN);
        long n = block + jetzt.toLocalDate().toEpochDay() * 7 + lage(k.t, k.active).ordinal() * 3L + tippe;

        List<String[]> liste;
        long i;
        if (szen.isEmpty()) {
            liste = basis;
            i = n;
        } else if (basis.isEmpty() || Math.floorMod(n, 2L) == 0) {
            // jedes zweite Mal ein Szenario-Spruch
            liste = szen;
            i = Math.floorDiv(n, 2L);
        } else {
            liste = basis;
            i = Math.floorDiv(n, 2L);
        }
        String[] s = liste.get((int) Math.floorMod(i, (long) liste.size()));
        return new Spruch(s[0], fuellen(s[1], k));
    }

    private static List<String[]> filter(List<String[]> in, Kontext k) {
        List<String[]> out = new ArrayList<>();
        for (String[] s : in) if (passt(s[1], k)) out.add(s);
        return out;
    }

    private static final String[] WOCHENTAGE = {
            "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"};

    /** Ersetzt die Platzhalter durch echte Werte. */
    static String fuellen(String text, Kontext k) {
        Work.Today t = k.t;
        LocalDateTime jetzt = LocalDateTime.ofInstant(Instant.ofEpochMilli(k.now), k.zone);
        String s = text
                .replace("{restmin}", String.valueOf(Math.max(0, -t.diff) / Work.MIN))
                .replace("{rest}", dauer(-t.diff))
                .replace("{plus}", dauer(t.diff))
                .replace("{netto}", dauer(t.dayNet))
                .replace("{grenze}", dauer(t.left))
                .replace("{um}", t.plusAt > 0 ? Fmt.time(t.plusAt) : "")
                .replace("{soll}", Fmt.hm(t.soll))
                .replace("{prozent}", String.valueOf(t.soll > 0 ? Math.round(t.dayNet * 100.0 / t.soll) : 0))
                .replace("{bisPause}", dauer(k.pauseAfterMin * Work.MIN - t.sessionGross))
                .replace("{einsatz}", String.valueOf(k.earlierSessions + 1))
                .replace("{latest}", k.latestOutMin >= 0
                        ? String.format(Locale.GERMANY, "%02d:%02d", k.latestOutMin / 60, k.latestOutMin % 60) : "")
                .replace("{woche}", Fmt.signed(k.weekDiff) + " h")
                .replace("{auszahlung}", String.format(Locale.GERMANY, "%,.0f €", k.payoutEuro))
                .replace("{zuschlag}", String.valueOf(k.weekPct))
                .replace("{wochentag}", WOCHENTAGE[jetzt.getDayOfWeek().getValue() - 1]);
        // "… noch 30 Min.." → ein Punkt reicht; "noch 30 Min. Jetzt" bleibt
        return s.replace("Min..", "Min.");
    }

    /** "45 Min." oder "1:23 h". */
    static String dauer(long ms) {
        long min = Math.max(0, ms) / Work.MIN;
        if (min <= 60) return min + " Min.";
        return String.format(Locale.GERMANY, "%d:%02d h", min / 60, min % 60);
    }
}
