package eus.ehu.dasproyecto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private Spinner spinnerLanguage;
    private EditText etWeeklyHours;
    private EditText etWorkingDays;
    private Button btnSave;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        etWeeklyHours = findViewById(R.id.etWeeklyHours);
        etWorkingDays = findViewById(R.id.etWorkingDays);
        btnSave = findViewById(R.id.btnSaveSettings);

        String[] languages = {"Español", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);

        loadSavedSettings();

        btnSave.setOnClickListener(v -> {
            String selectedLanguage = spinnerLanguage.getSelectedItemPosition() == 0 ? "es" : "en";

            String weeklyHoursStr = etWeeklyHours.getText().toString();
            String workingDaysStr = etWorkingDays.getText().toString();

            if (weeklyHoursStr.isEmpty() || workingDaysStr.isEmpty()) {
                Toast.makeText(this, getString(R.string.settings_incomplete),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            float weeklyHours = Float.parseFloat(weeklyHoursStr);
            int workingDays = Integer.parseInt(workingDaysStr);

            if (weeklyHours <= 0 || weeklyHours > 168 || workingDays <= 0 || workingDays > 7) {
                Toast.makeText(this, getString(R.string.invalid_settings),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            saveLanguagePreference(selectedLanguage);
            dbHelper.saveSettings(weeklyHours, workingDays);

            setAppLocale(selectedLanguage);
            restartApp();
        });
    }

    private void loadSavedSettings() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        spinnerLanguage.setSelection(lang.equals("es") ? 0 : 1);

        float[] settings = dbHelper.getSettings();
        etWeeklyHours.setText(String.valueOf(settings[0]));
        etWorkingDays.setText(String.valueOf((int)settings[1]));
    }

    private void saveLanguagePreference(String lang) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", lang);
        editor.apply();
    }

    private void setAppLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(this, getString(R.string.settings_updated), Toast.LENGTH_LONG).show();
    }
}
