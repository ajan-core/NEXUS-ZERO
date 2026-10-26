package com.janixia.ai;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(9, 10, 13), panel = Color.rgb(24, 24, 27),
        panel2 = Color.rgb(34, 33, 29), gold = Color.rgb(226, 187, 91),
        goldLight = Color.rgb(255, 225, 145), ink = Color.rgb(248, 246, 240),
        muted = Color.rgb(166, 164, 156);
    LinearLayout root, messages;
    EditText prompt, keyInput, modelInput, endpointInput;
    Spinner providerSpinner;
    TextView status;
    ScrollView scroll;
    SharedPreferences prefs;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    boolean waiting = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        prefs = getSharedPreferences("janixia_settings", MODE_PRIVATE);
        showSplash();
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    GradientDrawable outline(int color, int stroke, int radius) {
        GradientDrawable d = shape(color, radius); d.setStroke(dp(1), stroke); return d;
    }
    int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setPadding(dp(10), dp(7), dp(10), dp(7)); return t;
    }
    Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(bg);
        b.setTextSize(13); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false); b.setPadding(dp(12), dp(4), dp(12), dp(4));
        b.setBackground(shape(gold, 18)); return b;
    }
    EditText edit(String hint, String value, boolean secret) {
        EditText e = new EditText(this); e.setSingleLine(!secret); e.setText(value); e.setHint(hint);
        e.setHintTextColor(muted); e.setTextColor(ink); e.setTextSize(15);
        e.setPadding(dp(14), dp(10), dp(14), dp(10)); e.setBackground(outline(panel, Color.rgb(57,55,48), 16));
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }
    LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }
    void gap(LinearLayout into, int height) { View v = new View(this); into.addView(v, lp(1, height)); }

    void showSplash() {
        LinearLayout splash = column(); splash.setGravity(Gravity.CENTER); splash.setPadding(dp(28), dp(28), dp(28), dp(28));
        splash.setBackgroundColor(bg); setContentView(splash);
        TextView mark = text("J", 76, goldLight, true); mark.setGravity(Gravity.CENTER);
        GradientDrawable badge = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(47,37,17), Color.rgb(15,15,14), Color.rgb(38,29,13)});
        badge.setCornerRadius(dp(36)); badge.setStroke(dp(2), gold);
        mark.setBackground(badge); splash.addView(mark, lp(142,142));
        gap(splash, 22);
        TextView name = text("JANIXIA AI", 27, ink, true); name.setGravity(Gravity.CENTER); name.setLetterSpacing(0.12f);
        splash.addView(name);
        TextView sub = text("YOUR PERSONAL AI STUDIO", 11, gold, true); sub.setGravity(Gravity.CENTER); sub.setLetterSpacing(0.13f);
        splash.addView(sub);
        gap(splash, 34);
        ProgressBar progress = new ProgressBar(this); progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(gold));
        splash.addView(progress, lp(28,28));
        new Handler(Looper.getMainLooper()).postDelayed(this::showChat, 900);
    }

    void base(String eyebrow, String title, String subtitle) {
        root = column(); root.setPadding(dp(18), dp(14), dp(18), dp(12)); root.setBackgroundColor(bg); setContentView(root);
        LinearLayout brand = new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("J", 26, goldLight, true); mark.setGravity(Gravity.CENTER);
        mark.setBackground(outline(Color.rgb(35,29,17), gold, 14)); brand.addView(mark, lp(46,46));
        LinearLayout names = column(); names.setPadding(dp(10), 0, 0, 0);
        TextView logo = text("JANIXIA AI", 18, ink, true); logo.setLetterSpacing(0.05f); names.addView(logo);
        names.addView(text("PERSONAL AI STUDIO", 9, gold, true)); brand.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView dot = text("●", 11, prefs.getString("key","").isEmpty() ? muted : gold, true); dot.setGravity(Gravity.CENTER);
        brand.addView(dot, lp(26,38)); root.addView(brand);
        gap(root, 16);
        root.addView(text(eyebrow.toUpperCase(), 10, gold, true));
        root.addView(text(title, 25, ink, true));
        root.addView(text(subtitle, 13, muted, false));
        gap(root, 10);
    }

    void showChat() {
        base("WELCOME BACK", "What shall we create?", "Your ideas, with a little intelligence.");
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER_VERTICAL);
        Button settings = button("⚙  Settings"); settings.setOnClickListener(v -> showSettings());
        Button fresh = button("＋  New chat"); fresh.setOnClickListener(v -> { messages.removeAllViews(); addMessage("JANIXIA", "A fresh start. What would you like to explore?"); });
        nav.addView(settings, new LinearLayout.LayoutParams(0, dp(46), 1));
        View spacer = new View(this); nav.addView(spacer, lp(8,1));
        nav.addView(fresh, new LinearLayout.LayoutParams(0, dp(46), 1)); root.addView(nav);
        gap(root, 8);
        scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        messages = column(); messages.setPadding(dp(1), dp(4), dp(1), dp(10)); scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout composer = column(); composer.setPadding(dp(10), dp(8), dp(10), dp(8));
        composer.setBackground(outline(panel, Color.rgb(57,55,48), 22));
        prompt = new EditText(this); prompt.setHint("Message JANIXIA…"); prompt.setHintTextColor(muted);
        prompt.setTextColor(ink); prompt.setTextSize(15); prompt.setMinLines(1); prompt.setMaxLines(4);
        prompt.setBackgroundColor(Color.TRANSPARENT); prompt.setPadding(dp(6), dp(4), dp(6), dp(4));
        composer.addView(prompt, lp(-1,-2));
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(text("✦  Gemini-ready", 11, muted, false), new LinearLayout.LayoutParams(0, dp(36), 1));
        Button send = button("Send  ↑"); send.setOnClickListener(v -> sendMessage()); actions.addView(send, lp(-2,42));
        composer.addView(actions); root.addView(composer);
        TextView foot = text("AI can make mistakes · Check important information", 10, muted, false); foot.setGravity(Gravity.CENTER);
        root.addView(foot);
        addMessage("JANIXIA", "Welcome to your personal AI studio. Add your Gemini API key in Settings to begin. Your API key is not included in this app.");
    }

    void addMessage(String who, String body) {
        boolean assistant = who.equals("JANIXIA");
        LinearLayout bubble = column(); bubble.setPadding(dp(13), dp(10), dp(13), dp(10));
        bubble.setBackground(outline(assistant ? panel : Color.rgb(39,33,20), assistant ? Color.rgb(54,51,43) : Color.rgb(107,83,37), 18));
        TextView label = text(assistant ? "✦  JANIXIA" : "YOU", 10, assistant ? gold : goldLight, true);
        bubble.addView(label);
        TextView bodyView = text(body, 14, ink, false); bodyView.setPadding(dp(10), dp(4), dp(10), dp(6)); bodyView.setLineSpacing(dp(2), 1f);
        bubble.addView(bodyView);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(5), 0, dp(7)); messages.addView(bubble, p);
        if (scroll != null) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    void showSettings() {
        base("PERSONALIZE", "Connect your AI", "Use your own API key. Never share it with anyone.");
        ScrollView formScroll = new ScrollView(this); LinearLayout form = column();
        form.addView(text("AI PROVIDER", 11, gold, true));
        providerSpinner = new Spinner(this);
        String[] providers = {"Google Gemini", "OpenAI-compatible API"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, providers);
        providerSpinner.setAdapter(adapter); providerSpinner.setSelection(prefs.getString("provider","gemini").equals("openai") ? 1 : 0);
        form.addView(providerSpinner, lp(-1,48)); gap(form, 10);
        form.addView(text("API KEY", 11, gold, true));
        keyInput = edit("Paste your API key", prefs.getString("key",""), true); form.addView(keyInput); gap(form, 10);
        form.addView(text("MODEL NAME", 11, gold, true));
        modelInput = edit("gemini-2.5-flash", prefs.getString("model","gemini-2.5-flash"), false); form.addView(modelInput); gap(form, 10);
        form.addView(text("API ENDPOINT (OPENAI-COMPATIBLE)", 11, gold, true));
        endpointInput = edit("https://api.openai.com/v1/chat/completions", prefs.getString("endpoint","https://api.openai.com/v1/chat/completions"), false);
        form.addView(endpointInput); gap(form, 10);
        status = text("", 12, goldLight, false); form.addView(status);
        Button save = button("Save settings"); save.setOnClickListener(v -> {
            boolean openai = providerSpinner.getSelectedItemPosition() == 1;
            prefs.edit().putString("provider", openai ? "openai" : "gemini")
                .putString("key", keyInput.getText().toString().trim())
                .putString("model", modelInput.getText().toString().trim())
                .putString("endpoint", endpointInput.getText().toString().trim()).apply();
            status.setText("Settings saved on this device.");
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        });
        form.addView(save, lp(-1,48)); gap(form, 8);
        Button test = button("Test API connection"); test.setOnClickListener(v -> testConnection());
        form.addView(test, lp(-1,48)); gap(form, 8);
        Button back = button("Back to JANIXIA"); back.setOnClickListener(v -> showChat()); form.addView(back, lp(-1,48)); gap(form,8);
        Button delete = button("Remove saved API key"); delete.setOnClickListener(v -> {
            prefs.edit().remove("key").apply(); keyInput.setText(""); status.setText("API key removed.");
        }); form.addView(delete, lp(-1,48));
        formScroll.addView(form); root.addView(formScroll, new LinearLayout.LayoutParams(-1,0,1));
    }

    void testConnection() {
        String key = keyInput.getText().toString().trim();
        if (key.isEmpty()) { status.setText("Paste your API key first."); return; }
        status.setText("Testing connection…");
        prefs.edit().putString("provider", providerSpinner.getSelectedItemPosition() == 1 ? "openai" : "gemini")
            .putString("key", key).putString("model", modelInput.getText().toString().trim())
            .putString("endpoint", endpointInput.getText().toString().trim()).apply();
        executor.execute(() -> {
            String result;
            try { String reply = callApi("Reply with only: JANIXIA connection successful"); result = reply; }
            catch (Exception e) { result = "Connection failed: " + e.getMessage(); }
            final String finalResult = result;
            runOnUiThread(() -> { if (status != null) status.setText(finalResult); });
        });
    }

    void sendMessage() {
        if (waiting) return;
        String q = prompt.getText().toString().trim(); if (q.isEmpty()) return;
        String key = prefs.getString("key", "");
        if (key.isEmpty()) { addMessage("JANIXIA", "Please open Settings and add your own API key first."); return; }
        prompt.setText("");
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(prompt.getWindowToken(), 0);
        addMessage("YOU", q); addMessage("JANIXIA", "Thinking…"); waiting = true;
        executor.execute(() -> {
            String answer;
            try { answer = callApi(q); } catch (Exception e) { answer = "Request failed: " + e.getMessage() + "\nCheck your key, model, endpoint and internet connection."; }
            final String result = answer;
            runOnUiThread(() -> {
                int n = messages.getChildCount(); if (n > 0) messages.removeViewAt(n - 1);
                addMessage("JANIXIA", result); waiting = false;
            });
        });
    }

    String callApi(String q) throws Exception {
        String provider = prefs.getString("provider", "gemini"), key = prefs.getString("key", "");
        String model = prefs.getString("model", provider.equals("gemini") ? "gemini-2.5-flash" : "gpt-4o-mini");
        String urlString, body;
        if (provider.equals("gemini")) {
            urlString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + key;
            JSONObject part = new JSONObject().put("text", q);
            body = new JSONObject().put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(part)))).toString();
        } else {
            urlString = prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions");
            body = new JSONObject().put("model", model).put("messages", new JSONArray().put(new JSONObject().put("role","user").put("content",q))).toString();
        }
        HttpURLConnection c = (HttpURLConnection)new URL(urlString).openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(45000);
        c.setRequestProperty("Content-Type","application/json");
        if (provider.equals("openai")) c.setRequestProperty("Authorization","Bearer " + key);
        c.setDoOutput(true);
        try (OutputStream os = c.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buf = new byte[4096]; int n;
        while ((n = is.read(buf)) > 0) out.write(buf,0,n);
        is.close(); c.disconnect();
        String response = new String(out.toByteArray(), StandardCharsets.UTF_8);
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + response);
        JSONObject json = new JSONObject(response);
        if (provider.equals("gemini")) return json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
        return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }
    @Override protected void onDestroy() { super.onDestroy(); executor.shutdownNow(); }
}