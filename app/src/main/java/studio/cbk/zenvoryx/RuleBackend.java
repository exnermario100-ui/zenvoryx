package studio.cbk.zenvoryx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regel-Baustein ohne Internet: versteht feste Satzanfänge und Stichwörter. */
public class RuleBackend implements AiBackend {

    public static final String HELP = "Das verstehe ich noch nicht. Probier: \"öffne Chrome\", "
            + "\"suche Wetter\", \"timer 5\", \"wecker 7:30\", \"taschenlampe an\", \"wlan\", "
            + "\"lautstärke\", \"ruf 12345 an\", \"deinstalliere App\", \"einstellungen\" oder \"apps\".";

    private static final String[] OPEN = {"öffne ", "starte ", "open "};
    private static final String[] SEARCH = {"suche nach ", "suche ", "google ", "search "};
    private static final String[] UNINSTALL = {"deinstalliere ", "lösche app ", "uninstall "};
    private static final String[] DIAL = {"ruf ", "wähle ", "call "};
    private static final Pattern NUMBER = Pattern.compile("(\\d{1,4})");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})\\s*[:.]\\s*(\\d{2})");
    private static final Pattern PHONE = Pattern.compile("([+0-9][0-9 /\\-]{2,})");

    @Override
    public String handle(String command, Actions actions) {
        String original = command.trim();
        String c = original.toLowerCase();

        for (String p : OPEN) {
            if (c.startsWith(p)) return actions.openApp(c.substring(p.length()));
        }
        for (String p : SEARCH) {
            if (c.startsWith(p)) return actions.search(original.substring(p.length()));
        }
        for (String p : UNINSTALL) {
            if (c.startsWith(p)) return actions.uninstallApp(c.substring(p.length()));
        }
        for (String p : DIAL) {
            if (c.startsWith(p)) {
                Matcher m = PHONE.matcher(c.substring(p.length()));
                if (m.find()) return actions.dial(m.group(1));
                return "Welche Nummer soll ich wählen?";
            }
        }
        if (c.contains("wecker")) {
            Matcher t = TIME.matcher(c);
            if (t.find()) {
                return actions.alarm(Integer.parseInt(t.group(1)), Integer.parseInt(t.group(2)));
            }
            Matcher n = NUMBER.matcher(c);
            if (n.find()) return actions.alarm(Integer.parseInt(n.group(1)), 0);
            return "Wann soll der Wecker klingeln? Zum Beispiel: wecker 7:30";
        }
        if (c.contains("timer")) {
            Matcher m = NUMBER.matcher(c);
            if (m.find()) return actions.timer(Integer.parseInt(m.group(1)));
            return "Wie viele Minuten soll der Timer laufen?";
        }
        if (c.contains("taschenlampe")) return actions.torch(!c.contains("aus"));
        if (c.contains("wlan") || c.contains("wifi")) return actions.wifi();
        if (c.contains("lautstärke")) return actions.volume();
        if (c.contains("einstellungen")) return actions.settings();
        if (c.equals("apps")) return actions.listApps();
        return HELP;
    }
}
