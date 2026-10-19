package studio.cbk.zenvoryx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.Settings;

import java.util.List;
import java.util.Locale;

/** Alles, was Zenvoryx tun darf. Kritische Aktionen laufen immer über confirm(). */
public class Actions {

    private final Activity act;
    private final PackageManager pm;
    private final List<ResolveInfo> apps;

    public Actions(Activity act, List<ResolveInfo> apps) {
        this.act = act;
        this.pm = act.getPackageManager();
        this.apps = apps;
    }

    private void start(Intent i) {
        act.runOnUiThread(() -> {
            try {
                act.startActivity(i);
            } catch (ActivityNotFoundException ignored) {
            }
        });
    }

    /** Fragt den Nutzer, bevor etwas Kritisches passiert. */
    public void confirm(String question, Runnable onYes) {
        act.runOnUiThread(() -> new AlertDialog.Builder(act)
                .setMessage(question)
                .setPositiveButton("Ja", (d, w) -> onYes.run())
                .setNegativeButton("Abbrechen", null)
                .show());
    }

    public String listApps() {
        StringBuilder sb = new StringBuilder();
        for (ResolveInfo r : apps) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(r.loadLabel(pm));
        }
        return sb.toString();
    }

    public String openApp(String name) {
        String want = name.trim().toLowerCase();
        if (want.isEmpty()) return "Welche App soll ich öffnen?";
        for (ResolveInfo r : apps) {
            String label = r.loadLabel(pm).toString();
            if (label.toLowerCase().contains(want)) {
                Intent i = pm.getLaunchIntentForPackage(r.activityInfo.packageName);
                if (i != null) {
                    start(i);
                    return "Öffne " + label;
                }
            }
        }
        return "Ich finde keine App namens \"" + name.trim() + "\".";
    }

    public String search(String query) {
        String q = query.trim();
        if (q.isEmpty()) return "Wonach soll ich suchen?";
        String url = "https://duckduckgo.com/?q=" + Uri.encode(q);
        start(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        return "Suche nach \"" + q + "\"";
    }

    public String timer(int minutes) {
        if (minutes <= 0) return "Wie viele Minuten?";
        Intent i = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Zenvoryx")
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        start(i);
        return "Stelle einen Timer auf " + minutes + " Minuten.";
    }

    public String alarm(int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return "Diese Uhrzeit verstehe ich nicht.";
        }
        Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Zenvoryx")
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        start(i);
        return String.format(Locale.GERMANY, "Stelle den Wecker auf %02d:%02d.", hour, minute);
    }

    public String torch(boolean on) {
        try {
            CameraManager cm = (CameraManager) act.getSystemService(Context.CAMERA_SERVICE);
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics ch = cm.getCameraCharacteristics(id);
                Boolean flash = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
                if (flash != null && flash && facing != null
                        && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cm.setTorchMode(id, on);
                    return on ? "Taschenlampe an." : "Taschenlampe aus.";
                }
            }
            return "Ich finde keine Taschenlampe.";
        } catch (CameraAccessException | RuntimeException e) {
            return "Die Taschenlampe geht gerade nicht.";
        }
    }

    public String wifi() {
        start(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY));
        return "Öffne die WLAN-Auswahl.";
    }

    public String volume() {
        start(new Intent(Settings.Panel.ACTION_VOLUME));
        return "Öffne die Lautstärke.";
    }

    public String settings() {
        start(new Intent(Settings.ACTION_SETTINGS));
        return "Öffne die Einstellungen.";
    }

    public String dial(String number) {
        String n = number.replaceAll("[^0-9+*#]", "");
        if (n.isEmpty()) return "Welche Nummer soll ich wählen?";
        start(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", n, null)));
        return "Öffne die Telefon-App mit " + n + ". Anrufen musst du selbst bestätigen.";
    }

    public void appInfo(String pkg) {
        start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", pkg, null)));
    }

    public void uninstallPackage(String pkg, String label) {
        confirm("\"" + label + "\" wirklich deinstallieren?", () ->
                start(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg))));
    }

    public String uninstallApp(String name) {
        String want = name.trim().toLowerCase();
        if (want.isEmpty()) return "Welche App soll ich deinstallieren?";
        for (ResolveInfo r : apps) {
            String label = r.loadLabel(pm).toString();
            if (label.toLowerCase().contains(want)) {
                uninstallPackage(r.activityInfo.packageName, label);
                return "Ich frage dich vorher noch einmal wegen \"" + label + "\".";
            }
        }
        return "Ich finde keine App namens \"" + name.trim() + "\".";
    }
}
