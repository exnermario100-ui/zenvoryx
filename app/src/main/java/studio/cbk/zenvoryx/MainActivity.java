package studio.cbk.zenvoryx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private final List<ResolveInfo> apps = new ArrayList<>();
    private final List<ResolveInfo> shown = new ArrayList<>();
    private PackageManager pm;
    private AiBackend backend;
    private Actions actions;
    private AppAdapter adapter;
    private TextView answerView;
    private ScrollView answerScroll;
    private EditText input;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        pm = getPackageManager();
        loadApps();
        backend = new RuleBackend();
        actions = new Actions(this, apps);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void loadApps() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        apps.clear();
        apps.addAll(pm.queryIntentActivities(i, 0));
        Collections.sort(apps, (a, c) ->
                a.loadLabel(pm).toString().compareToIgnoreCase(c.loadLabel(pm).toString()));
        applyFilter();
    }

    private void applyFilter() {
        String q = input == null ? "" : input.getText().toString().trim().toLowerCase();
        shown.clear();
        if (!q.isEmpty()) {
            for (ResolveInfo r : apps) {
                if (r.loadLabel(pm).toString().toLowerCase().contains(q)) shown.add(r);
            }
        }
        if (shown.isEmpty()) shown.addAll(apps);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void launch(ResolveInfo r) {
        Intent i = pm.getLaunchIntentForPackage(r.activityInfo.packageName);
        if (i != null) startActivity(i);
    }

    private void showAppMenu(ResolveInfo r) {
        final String pkg = r.activityInfo.packageName;
        final String label = r.loadLabel(pm).toString();
        new AlertDialog.Builder(this)
                .setTitle(label)
                .setItems(new String[]{"App-Info", "Deinstallieren"}, (d, which) -> {
                    if (which == 0) actions.appInfo(pkg);
                    else actions.uninstallPackage(pkg, label);
                })
                .show();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0f1117"));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets s = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return WindowInsets.CONSUMED;
        });

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(48);
        header.addView(clock);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d. MMMM");
        date.setFormat12Hour("EEEE, d. MMMM");
        date.setTextColor(Color.LTGRAY);
        date.setTextSize(15);
        header.addView(date);

        root.addView(header);

        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        adapter = new AppAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, pos, id) -> launch(shown.get(pos)));
        grid.setOnItemLongClickListener((parent, view, pos, id) -> {
            showAppMenu(shown.get(pos));
            return true;
        });
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        answerScroll = new ScrollView(this);
        answerView = new TextView(this);
        answerView.setTextColor(Color.WHITE);
        answerView.setTextSize(15);
        answerView.setPadding(dp(16), dp(8), dp(16), dp(8));
        answerView.setText("Willkommen bei Zenvoryx 0.2! Tippe, um Apps zu suchen, oder gib einen "
                + "Befehl ein, z. B. \"öffne Chrome\", \"timer 5\" oder \"taschenlampe an\".");
        answerScroll.addView(answerView);
        root.addView(answerScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(100)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(4), dp(8), dp(8));

        input = new EditText(this);
        input.setHint("App suchen oder Befehl eingeben …");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter();
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            send();
            return true;
        });
        row.addView(input, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button sendBtn = new Button(this);
        sendBtn.setText("Senden");
        sendBtn.setOnClickListener(v -> send());
        row.addView(sendBtn);

        root.addView(row);
        setContentView(root);
    }

    private void show(String s) {
        answerView.setText(s);
        answerScroll.post(() -> answerScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void send() {
        final String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        show("Du: " + text + "\n\nZenvoryx denkt …");
        new Thread(() -> {
            String result;
            try {
                result = backend.handle(text, actions);
            } catch (Exception ex) {
                result = "Fehler: " + ex.getMessage();
            }
            final String out = result;
            runOnUiThread(() -> show("Du: " + text + "\n\nZenvoryx: " + out));
        }).start();
    }

    private class AppAdapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int p) { return shown.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int p, View convert, ViewGroup parent) {
            LinearLayout cell = new LinearLayout(MainActivity.this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(4), dp(10), dp(4), dp(10));
            ResolveInfo r = shown.get(p);

            ImageView icon = new ImageView(MainActivity.this);
            icon.setImageDrawable(r.loadIcon(pm));
            cell.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

            TextView label = new TextView(MainActivity.this);
            label.setText(r.loadLabel(pm));
            label.setTextColor(Color.WHITE);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            cell.addView(label);
            return cell;
        }
    }
}
