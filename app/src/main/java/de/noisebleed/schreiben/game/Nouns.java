package de.noisebleed.schreiben.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Nomen mit Artikel und Mehrzahl für das Spiel Mehrzahl.
 *
 * Grundlage ist der Grundwortschatz für die Jahrgangsstufen 1 und 2 (LehrplanPLUS Bayern): alle
 * Nomen daraus, die eine gebräuchliche Mehrzahl haben ({@link #gws}). Weggelassen sind Wörter ohne
 * sinnvolle Mehrzahl (Salz, Gemüse, Eis, Sport, Regen, Wasser, Frühling, Quatsch …) oder mit
 * unsicherer (Euro, Cent, Himmel, Nebel). Dazu kommen häufige Kinderwörter, deren Mehrzahl einen
 * Umlaut bekommt ({@link #extra}): Hand – Hände, Ball – Bälle …, weil gerade das geübt werden soll.
 *
 * Hat ein Wort zwei richtige Mehrzahlformen, zählen beide; die erste wird als Lösung gezeigt.
 */
public final class Nouns {

    /** Ein Nomen: "der Apfel" mit Mehrzahl "Äpfel". */
    public static final class Noun {
        public final String article;
        public final String singular;
        public final List<String> plurals;
        /** Steht im Grundwortschatz der Jahrgangsstufen 1 und 2. */
        public final boolean basic;

        Noun(String article, String singular, boolean basic, String... plurals) {
            this.article = article;
            this.singular = singular;
            this.basic = basic;
            this.plurals = Collections.unmodifiableList(Arrays.asList(plurals));
        }

        /** Die Mehrzahl bekommt einen Umlaut, den die Einzahl nicht hat: Apfel – Äpfel, Haus – Häuser. */
        public boolean umlaut() {
            return !hasUmlaut(singular) && hasUmlaut(plurals.get(0));
        }

        public Task toTask() {
            return new Task(article + " " + singular, "die", plurals);
        }

        private static boolean hasUmlaut(String word) {
            return word.matches(".*[äöüÄÖÜ].*");
        }
    }

    public static final List<Noun> ALL;

    static {
        List<Noun> all = new ArrayList<>(Arrays.asList(
                // Silben mitsprechen
                gws("die", "Aufgabe", "Aufgaben"),
                gws("das", "Auge", "Augen"),
                gws("das", "Auto", "Autos"),
                gws("die", "Blume", "Blumen"),
                gws("die", "Blüte", "Blüten"),
                gws("das", "Brot", "Brote"),
                gws("die", "Dose", "Dosen"),
                gws("die", "Ente", "Enten"),
                gws("die", "Frau", "Frauen"),
                gws("der", "Hase", "Hasen"),
                gws("die", "Hose", "Hosen"),
                gws("die", "Kiste", "Kisten"),
                gws("der", "Löwe", "Löwen"),
                gws("der", "Name", "Namen"),
                gws("die", "Oma", "Omas"),
                gws("der", "Opa", "Opas"),
                gws("der", "Rabe", "Raben"),
                gws("die", "Raupe", "Raupen"),
                gws("das", "Schaf", "Schafe"),
                gws("die", "Schere", "Scheren"),
                gws("die", "Schule", "Schulen"),
                gws("die", "Tante", "Tanten"),
                gws("das", "Telefon", "Telefone"),
                gws("die", "Tomate", "Tomaten"),
                gws("der", "Wolf", "Wölfe"),
                gws("die", "Wolke", "Wolken"),
                // Buchstabengruppen: er, el, ei, ng, nk, st, ch, sch, pf, qu, r nach Vokal
                gws("der", "Bruder", "Brüder"),
                gws("die", "Feder", "Federn"),
                gws("das", "Fenster", "Fenster"),
                gws("die", "Schwester", "Schwestern"),
                gws("der", "Winter", "Winter"),
                gws("der", "Esel", "Esel"),
                gws("die", "Gabel", "Gabeln"),
                gws("die", "Nadel", "Nadeln"),
                gws("der", "Onkel", "Onkel"),
                gws("der", "Pinsel", "Pinsel"),
                gws("die", "Wurzel", "Wurzeln"),
                gws("die", "Ameise", "Ameisen"),
                gws("das", "Ei", "Eier"),
                gws("die", "Seife", "Seifen"),
                gws("die", "Zeit", "Zeiten"),
                gws("der", "Junge", "Jungen"),
                gws("der", "Ring", "Ringe"),
                gws("die", "Bank", "Bänke", "Banken"),
                gws("der", "Stein", "Steine"),
                gws("der", "Stern", "Sterne"),
                gws("die", "Stunde", "Stunden"),
                gws("die", "Woche", "Wochen"),
                gws("die", "Tasche", "Taschen"),
                gws("der", "Tisch", "Tische"),
                gws("der", "Kopf", "Köpfe"),
                gws("die", "Pflanze", "Pflanzen"),
                gws("die", "Quelle", "Quellen"),
                gws("das", "Quadrat", "Quadrate"),
                gws("die", "Birne", "Birnen"),
                gws("der", "Garten", "Gärten"),
                gws("der", "Partner", "Partner"),
                gws("das", "Wort", "Wörter", "Worte"),
                // ie und Mitlautverdopplung
                gws("die", "Biene", "Bienen"),
                gws("die", "Wiese", "Wiesen"),
                gws("die", "Ziege", "Ziegen"),
                gws("der", "Füller", "Füller"),
                gws("die", "Klasse", "Klassen"),
                gws("die", "Mutter", "Mütter"),
                gws("die", "Sonne", "Sonnen"),
                // Umlautung und Verhärtung
                gws("der", "Apfel", "Äpfel"),
                gws("der", "Baum", "Bäume"),
                gws("das", "Gras", "Gräser"),
                gws("das", "Haus", "Häuser"),
                gws("die", "Maus", "Mäuse"),
                gws("das", "Bild", "Bilder"),
                gws("der", "Bub", "Buben"),
                gws("der", "Freund", "Freunde"),
                gws("der", "Hund", "Hunde"),
                gws("das", "Kind", "Kinder"),
                gws("das", "Kleid", "Kleider"),
                gws("das", "Pferd", "Pferde"),
                gws("der", "Tag", "Tage"),
                gws("der", "Weg", "Wege"),
                gws("der", "Wind", "Winde"),
                // Rechtschreibbesonderheiten
                gws("der", "Fuß", "Füße"),
                gws("die", "Katze", "Katzen"),
                gws("der", "Satz", "Sätze"),
                gws("der", "Clown", "Clowns"),
                gws("der", "Computer", "Computer"),
                gws("die", "Vase", "Vasen"),
                gws("der", "Vater", "Väter"),
                gws("der", "Vogel", "Vögel"),
                gws("das", "Baby", "Babys"),
                gws("die", "Hexe", "Hexen"),
                gws("das", "Jahr", "Jahre"),
                gws("die", "Uhr", "Uhren"),
                gws("die", "Zahl", "Zahlen"),
                gws("der", "Zahn", "Zähne"),
                gws("das", "Mädchen", "Mädchen"),
                gws("der", "Hai", "Haie"),
                gws("der", "Kaiser", "Kaiser"),

                // Nicht im Grundwortschatz: häufige Wörter mit Umlaut in der Mehrzahl
                extra("die", "Hand", "Hände"),
                extra("der", "Ball", "Bälle"),
                extra("die", "Nuss", "Nüsse"),
                extra("der", "Stuhl", "Stühle"),
                extra("der", "Zug", "Züge"),
                extra("der", "Hut", "Hüte"),
                extra("die", "Kuh", "Kühe"),
                extra("die", "Gans", "Gänse"),
                extra("der", "Turm", "Türme"),
                extra("das", "Dach", "Dächer"),
                extra("das", "Glas", "Gläser"),
                extra("das", "Rad", "Räder"),
                extra("das", "Blatt", "Blätter"),
                extra("der", "Wald", "Wälder"),
                extra("der", "Topf", "Töpfe"),
                extra("der", "Frosch", "Frösche"),
                extra("der", "Fluss", "Flüsse"),
                extra("die", "Stadt", "Städte"),
                extra("die", "Nacht", "Nächte"),
                extra("die", "Wurst", "Würste"),
                extra("der", "Bauch", "Bäuche"),
                extra("der", "Schrank", "Schränke"),
                extra("das", "Loch", "Löcher"),
                extra("das", "Dorf", "Dörfer"),
                extra("der", "Sohn", "Söhne"),
                extra("die", "Tochter", "Töchter"),
                extra("der", "Mantel", "Mäntel"),
                extra("das", "Huhn", "Hühner"),
                extra("der", "Fuchs", "Füchse"),
                extra("der", "Korb", "Körbe"),
                extra("der", "Saft", "Säfte"),
                extra("der", "Bart", "Bärte"),
                extra("der", "Hals", "Hälse"),
                extra("der", "Zaun", "Zäune")
        ));
        ALL = Collections.unmodifiableList(all);
    }

    private Nouns() {
    }

    private static Noun gws(String article, String singular, String... plurals) {
        return new Noun(article, singular, true, plurals);
    }

    private static Noun extra(String article, String singular, String... plurals) {
        return new Noun(article, singular, false, plurals);
    }
}
