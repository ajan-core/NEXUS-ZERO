package com.janixia.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
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
    int bg = Color.rgb(10, 13, 24), panel = Color.rgb(22, 27, 44), accent = Color.rgb(117, 94, 255), ink = Color.WHITE;
    LinearLayout root, messages;
    EditText prompt, keyInput, modelInput, endpointInput;
    Spinner providerSpinner;
    TextView status;
    ScrollView scroll;
    SharedPreferences prefs;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("janixia_settings", MODE_PRIVATE);
        showChat();
    }
    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(12, 8, 12, 8); return t;
    }
    Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setBackground(shape(accent, 22)); return b;
    }
    EditText edit(String hint, String value, boolean secret) {
        EditText e = new EditText(this); e.setSingleLine(!secret); e.setText(value); e.setHint(hint);
        e.setHintTextColor(Color.LTGRAY); e.setTextColor(Color.WHITE); e.setTextSize(15);
        e.setPadding(14, 10, 14, 10); e.setBackground(shape(panel, 18));
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }
    void base(String title, String subtitle) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16, 16, 16, 12); root.setBackgroundColor(bg);
        setContentView(root);
        root.addView(text("✦ JANIXIA AI", 24, Color.WHITE, true));
        root.addView(text(title, 18, Color.rgb(186, 176, 255), true));
        root.addView(text(subtitle, 12, Color.LTGRAY, false));
    }
    void showChat() {
        base("Your personal AI space", "Add your own provider API key in Settings to chat.");
        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        Button settings = button("⚙ API Settings"); settings.setOnClickListener(v -> showSettings());
        Button clear = button("Clear chat"); clear.setOnClickListener(v -> { messages.removeAllViews(); addMessage("JANIXIA", "Chat cleared."); });
        nav.addView(settings, new LinearLayout.LayoutParams(0, -2, 1)); nav.addView(clear, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(nav);
        scroll = new ScrollView(this); scroll.setFillViewport(true);
        messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); messages.setPadding(2, 10, 2, 10);
        scroll.addView(messages); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        prompt = edit("Ask JANIXIA anything…", "", false); row.addView(prompt, new LinearLayout.LayoutParams(0, -2, 1));
        Button send = button("Send"); send.setOnClickListener(v -> sendMessage()); row.addView(send);
        root.addView(row);
        addMessage("JANIXIA", "Welcome! Open API Settings, choose Google Gemini or an OpenAI-compatible provider, then add your own key. No paid API key is included.");
    }
    void addMessage(String who, String body) {
        TextView t = text(who + "\n" + body, 15, ink, false);
        t.setBackground(shape(panel, 18)); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 5, 0, 7); messages.addView(t, lp);
        if (scroll != null) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }
    void showSettings() {
        base("API Settings", "Your key is stored locally on this device. Never share it.");
        root.addView(text("Provider", 14, ink, true));
        providerSpinner = new Spinner(this);
        String[] providers = {"Google Gemini", "OpenAI-compatible API"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, providers);
        providerSpinner.setAdapter(adapter);
        providerSpinner.setSelection(prefs.getString("provider", "gemini").equals("openai") ? 1 : 0);
        root.addView(providerSpinner);
        root.addView(text("API Key", 14, ink, true));
        keyInput = edit("Paste your API key", prefs.getString("key", ""), true); root.addView(keyInput);
        root.addView(text("Model", 14, ink, true));
        modelInput = edit("Model name", prefs.getString("model", "gemini-2.5-flash"), false); root.addView(modelInput);
        root.addView(text("API endpoint (OpenAI-compatible only)", 14, ink, true));
        endpointInput = edit("https://api.openai.com/v1/chat/completions", prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions"), false); root.addView(endpointInput);
        status = text("", 12, Color.rgb(150, 235, 180), false); root.addView(status);
        Button save = button("Save settings"); save.setOnClickListener(v -> {
            boolean openai = providerSpinner.getSelectedItemPosition() == 1;
            prefs.edit().putString("provider", openai ? "openai" : "gemini")
                .putString("key", keyInput.getText().toString().trim())
                .putString("model", modelInput.getText().toString().trim())
                .putString("endpoint", endpointInput.getText().toString().trim()).apply();
            status.setText("Saved on this device.");
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        }); root.addView(save);
        Button back = button("Back to chat"); back.setOnClickListener(v -> showChat()); root.addView(back);
        Button delete = button("Remove saved API key"); delete.setOnClickListener(v -> {
            prefs.edit().remove("key").apply(); keyInput.setText(""); status.setText("API key removed.");
        }); root.addView(delete);
    }
    void sendMessage() {
        String q = prompt.getText().toString().trim(); if (q.isEmpty()) return;
        String key = prefs.getString("key", "");
        if (key.isEmpty()) { addMessage("JANIXIA", "Please add your own API key in API Settings first."); return; }
        prompt.setText(""); addMessage("YOU", q); addMessage("JANIXIA", "Thinking…");
        executor.execute(() -> {
            String answer;
            try { answer = callApi(q); } catch (Exception e) { answer = "Request failed: " + e.getMessage() + "\nCheck your key, model, endpoint and internet connection."; }
            final String result = answer;
            runOnUiThread(() -> {
                int n = messages.getChildCount();
                if (n > 0) messages.removeViewAt(n - 1);
                addMessage("JANIXIA", result);
            });
        });
    }
    String callApi(String q) throws Exception {
        String provider = prefs.getString("provider", "gemini");
        String key = prefs.getString("key", "");
        String model = prefs.getString("model", provider.equals("gemini") ? "gemini-2.5-flash" : "gpt-4o-mini");
        String urlString, body;
        if (provider.equals("gemini")) {
            urlString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + key;
            JSONObject part = new JSONObject().put("text", q);
            JSONArray parts = new JSONArray().put(part);
            JSONObject content = new JSONObject().put("parts", parts);
            body = new JSONObject().put("contents", new JSONArray().put(content)).toString();
        } else {
            urlString = prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions");
            JSONArray msgs = new JSONArray().put(new JSONObject().put("role", "user").put("content", q));
            body = new JSONObject().put("model", model).put("messages", msgs).toString();
        }
        HttpURLConnection c = (HttpURLConnection) new URL(urlString).openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(45000);
        c.setRequestProperty("Content-Type", "application/json");
        if (provider.equals("openai")) c.setRequestProperty("Authorization", "Bearer " + key);
        c.setDoOutput(true);
        try (OutputStream os = c.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buf = new byte[4096]; int n;
        while ((n = is.read(buf)) > 0) out.write(buf, 0, n);
        String response = new String(out.toByteArray(), StandardCharsets.UTF_8);
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + response);
        JSONObject json = new JSONObject(response);
        if (provider.equals("gemini")) return json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
        return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }
    @Override protected void onDestroy() { super.onDestroy(); executor.shutdownNow(); }
}
