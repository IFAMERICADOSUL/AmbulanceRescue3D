package com.frontiera.piccolafattoria;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import java.util.Locale;

public class MainActivity extends Activity {
    private int coins = 30, seeds = 5, water = 5, harvest = 0, level = 1;
    private int planted = 0, watered = 0;
    private TextView stats, field, message, levelText;
    private SharedPreferences save;

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        save = getSharedPreferences("farm_save", MODE_PRIVATE);
        load();
        buildUi();
        refresh();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(Color.rgb(239, 246, 229));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = label("🌻 PICCOLA FATTORIA", 25, Color.rgb(35,77,42), true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());
        TextView subtitle = label("La tua fattoria, le tue regole!", 14, Color.DKGRAY, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(5), 0, dp(16));
        root.addView(subtitle);

        LinearLayout statsCard = card();
        stats = label("", 16, Color.rgb(35,65,37), true);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(dp(12), dp(14), dp(12), dp(14));
        statsCard.addView(stats);
        root.addView(statsCard, matchWrap());

        LinearLayout farm = card();
        farm.setBackground(shape(Color.rgb(210, 183, 125), 18));
        TextView farmTitle = label("🌱 IL TUO CAMPO", 19, Color.rgb(74,48,22), true);
        farmTitle.setGravity(Gravity.CENTER);
        farmTitle.setPadding(0, dp(8), 0, dp(8));
        farm.addView(farmTitle);
        field = label("", 17, Color.rgb(64,48,22), true);
        field.setGravity(Gravity.CENTER);
        field.setPadding(dp(8), dp(18), dp(8), dp(18));
        farm.addView(field);
        root.addView(farm, matchWrap());

        levelText = label("", 15, Color.rgb(35,77,42), true);
        levelText.setGravity(Gravity.CENTER);
        levelText.setPadding(0, dp(12), 0, dp(8));
        root.addView(levelText);

        TextView actions = label("AZIONI", 18, Color.rgb(35,77,42), true);
        actions.setPadding(0, dp(14), 0, dp(8));
        root.addView(actions);

        addButton(root, "🌾 Semina un appezzamento  (2 semi)", v -> plant());
        addButton(root, "💧 Annaffia il campo  (1 acqua)", v -> waterField());
        addButton(root, "☀️ Aspetta che cresca", v -> grow());
        addButton(root, "🧺 Raccogli il raccolto", v -> collect());
        addButton(root, "🛒 Compra 5 semi  (10 monete)", v -> buySeeds());
        addButton(root, "🚰 Compra 5 unità d'acqua  (8 monete)", v -> buyWater());
        addButton(root, "🏡 Espandi la fattoria  (40 monete)", v -> expand());

        message = label("Consiglio: semina, annaffia e fai crescere le colture!", 14, Color.DKGRAY, false);
        message.setGravity(Gravity.CENTER);
        message.setPadding(dp(8), dp(14), dp(8), dp(8));
        root.addView(message);
        addButton(root, "↻ Nuova partita", v -> reset());
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(10), dp(12), dp(10));
        l.setBackground(shape(Color.WHITE, 18));
        return l;
    }

    private void addButton(LinearLayout root, String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(Color.rgb(57,125,59), 14));
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(7);
        p.bottomMargin = dp(3);
        root.addView(b, p);
    }

    private void say(String s) {
        message.setText(s);
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        save();
        refresh();
    }

    private void plant() {
        if (seeds < 2) { say("Non hai abbastanza semi. Comprane altri!"); return; }
        if (planted >= level + 1) { say("Il campo è pieno. Espandi la fattoria!"); return; }
        seeds -= 2; planted++; watered = Math.min(watered, planted);
        say("Hai seminato un appezzamento. Ora annaffialo!");
    }

    private void waterField() {
        int need = planted - watered;
        if (need <= 0) { say("Non ci sono colture da annaffiare."); return; }
        if (water <= 0) { say("Acqua esaurita. Comprane altra!"); return; }
        int amount = Math.min(need, water);
        water -= amount; watered += amount;
        say("Hai annaffiato " + amount + " appezzamento/i.");
    }

    private void grow() {
        if (planted == 0) { say("Prima devi seminare!"); return; }
        if (watered < planted) { say("Annaffia tutte le colture prima di farle crescere."); return; }
        harvest += planted * 2;
        watered = 0;
        say("Le colture sono cresciute! Puoi raccogliere " + harvest + " prodotti.");
    }

    private void collect() {
        if (harvest <= 0) { say("Non c'è ancora nulla da raccogliere."); return; }
        int amount = harvest;
        coins += amount * 3;
        harvest = 0;
        planted = 0; watered = 0;
        say("Ottimo raccolto: +" + (amount * 3) + " monete!");
    }

    private void buySeeds() {
        if (coins < 10) { say("Non hai abbastanza monete."); return; }
        coins -= 10; seeds += 5; say("Hai comprato 5 semi.");
    }

    private void buyWater() {
        if (coins < 8) { say("Non hai abbastanza monete."); return; }
        coins -= 8; water += 5; say("Hai riempito la riserva d'acqua.");
    }

    private void expand() {
        if (level >= 5) { say("Hai già raggiunto il livello massimo!"); return; }
        if (coins < 40) { say("Servono 40 monete per espandere."); return; }
        coins -= 40; level++; say("Fattoria ampliata! Ora puoi coltivare di più.");
    }

    private void refresh() {
        if (stats == null) return;
        stats.setText("🪙 " + coins + " monete     🌰 " + seeds + " semi\n💧 " + water +
                " acqua       🧺 " + harvest + " prodotti");
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < level + 1; i++) {
            if (i > 0) s.append("     ");
            if (i < planted) s.append(watered > i ? "🌿" : "🌱");
            else s.append("🟫");
        }
        s.append("\n\nColture: ").append(planted).append("/").append(level + 1)
         .append("     Annaffiate: ").append(watered);
        field.setText(s.toString());
        levelText.setText("🏡 Livello fattoria: " + level + " / 5");
    }

    private void save() {
        save.edit().putInt("coins", coins).putInt("seeds", seeds)
            .putInt("water", water).putInt("harvest", harvest)
            .putInt("level", level).putInt("planted", planted)
            .putInt("watered", watered).apply();
    }

    private void load() {
        coins = save.getInt("coins", 30);
        seeds = save.getInt("seeds", 5);
        water = save.getInt("water", 5);
        harvest = save.getInt("harvest", 0);
        level = save.getInt("level", 1);
        planted = save.getInt("planted", 0);
        watered = save.getInt("watered", 0);
    }

    private void reset() {
        coins = 30; seeds = 5; water = 5; harvest = 0;
        level = 1; planted = 0; watered = 0;
        say("Nuova fattoria creata. Buon divertimento!");
    }

    @Override protected void onPause() {
        super.onPause();
        save();
    }
}
