package eus.ehu.dasproyecto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private Spinner spinnerLanguage;
    private TextView tvHoursValue, tvMinutesValue;
    private Button btnIncreaseHours, btnDecreaseHours;
    private Button btnIncreaseMinutes, btnDecreaseMinutes;
    private Button btnSave;
    private Button btnDeleteHistory;
    private DatabaseHelper dbHelper;

    private ToggleButton toggleMonday, toggleTuesday, toggleWednesday,
            toggleThursday, toggleFriday, toggleSaturday, toggleSunday;
    private List<ToggleButton> dayToggles;

    private int selectedHours = 40;
    private int selectedMinutes = 0;

    private static final int MAX_HOURS = 168;
    private static final int MIN_HOURS = 0;
    private static final int MAX_MINUTES = 55;
    private static final int MIN_MINUTES = 0;
    private static final int MINUTE_INCREMENT = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        spinnerLanguage = findViewById(R.id.spinnerLanguage);

        tvHoursValue = findViewById(R.id.tvHoursValue);
        tvMinutesValue = findViewById(R.id.tvMinutesValue);
        btnIncreaseHours = findViewById(R.id.btnIncreaseHours);
        btnDecreaseHours = findViewById(R.id.btnDecreaseHours);
        btnIncreaseMinutes = findViewById(R.id.btnIncreaseMinutes);
        btnDecreaseMinutes = findViewById(R.id.btnDecreaseMinutes);

        btnDeleteHistory = findViewById(R.id.btnDeleteHistory);

        btnDeleteHistory.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        btnSave = findViewById(R.id.btnSaveSettings);

        initializeDayToggles();

        String[] languages = {"Español", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);

        loadSavedSettings();

        setupNumberPickers();

        btnSave.setOnClickListener(v -> {
            String selectedLanguage = spinnerLanguage.getSelectedItemPosition() == 0 ? "es" : "en";

            // Calcula horas semanales a trabajar en función de los parámetros de horas y días
            float weeklyHours = selectedHours + (selectedMinutes / 60.0f);

            // Contador de los días seleccionados
            int workingDays = countSelectedDays();

            if (weeklyHours <= 0 || weeklyHours > 168 || workingDays <= 0 || workingDays > 7) {
                Toast.makeText(this, getString(R.string.invalid_settings),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Almacenar preferencias
            saveLanguagePreference(selectedLanguage);
            dbHelper.saveSettings(weeklyHours, workingDays);

            // Reinicia la app si se cambia el idioma para que se use strings-en o el asociado
            setAppLocale(selectedLanguage);
            restartApp();
        });
    }

    // Configura el selector de horas y minutos a la semana
    private void setupNumberPickers() {
        btnIncreaseHours.setOnClickListener(v -> {
            if (selectedHours < MAX_HOURS) {
                selectedHours++;
                updateHoursDisplay();
            }
        });

        btnDecreaseHours.setOnClickListener(v -> {
            if (selectedHours > MIN_HOURS) {
                selectedHours--;
                updateHoursDisplay();
            }
        });

        // Control de minutos
        btnIncreaseMinutes.setOnClickListener(v -> {
            selectedMinutes += MINUTE_INCREMENT;
            if (selectedMinutes > MAX_MINUTES) {
                selectedMinutes = MIN_MINUTES;
                // Si los minutos exceden 60, sumar hora
                if (selectedHours < MAX_HOURS) {
                    selectedHours++;
                    updateHoursDisplay();
                }
            }
            updateMinutesDisplay();
        });

        btnDecreaseMinutes.setOnClickListener(v -> {
            selectedMinutes -= MINUTE_INCREMENT;
            if (selectedMinutes < MIN_MINUTES) {
                selectedMinutes = MAX_MINUTES;
                // Si los minutos son ya 0, restar hora
                if (selectedHours > MIN_HOURS) {
                    selectedHours--;
                    updateHoursDisplay();
                }
            }
            updateMinutesDisplay();
        });
    }

    private void updateHoursDisplay() {
        tvHoursValue.setText(String.valueOf(selectedHours));
    }

    private void updateMinutesDisplay() {
        tvMinutesValue.setText(String.format(Locale.getDefault(), "%02d", selectedMinutes));
    }

    private void initializeDayToggles() {
        toggleMonday = findViewById(R.id.toggleMonday);
        toggleTuesday = findViewById(R.id.toggleTuesday);
        toggleWednesday = findViewById(R.id.toggleWednesday);
        toggleThursday = findViewById(R.id.toggleThursday);
        toggleFriday = findViewById(R.id.toggleFriday);
        toggleSaturday = findViewById(R.id.toggleSaturday);
        toggleSunday = findViewById(R.id.toggleSunday);

        dayToggles = new ArrayList<>();
        dayToggles.add(toggleMonday);
        dayToggles.add(toggleTuesday);
        dayToggles.add(toggleWednesday);
        dayToggles.add(toggleThursday);
        dayToggles.add(toggleFriday);
        dayToggles.add(toggleSaturday);
        dayToggles.add(toggleSunday);
    }

    private int countSelectedDays() {
        int count = 0;
        for (ToggleButton toggle : dayToggles) {
            if (toggle.isChecked()) {
                count++;
            }
        }
        return count;
    }

    private void setSelectedDays(int days) {
        // Por defecto
        if (days <= 0 || days > 7) {
            days = 5;
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < 5); // Marcar lunes-viernes
            }
            return;
        }

        // Marcar lunes-viernes
        if (days == 5) {
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < 5);
            }
        } else if (days < 5) {
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < days);
            }
        } else {
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < days);
            }
        }
    }

    private void loadSavedSettings() {
        // Cargar idioma guardado
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        spinnerLanguage.setSelection(lang.equals("es") ? 0 : 1);

        // Mostrar el resto de ajustes
        float[] settings = dbHelper.getSettings();

        // Divisor de horas a horas y minutos
        selectedHours = (int) settings[0];

        // Aplicar redondeo para tener minutos de 5 en 5
        selectedMinutes = Math.round((settings[0] - selectedHours) * 60 / MINUTE_INCREMENT) * MINUTE_INCREMENT;

        updateHoursDisplay();
        updateMinutesDisplay();

        setSelectedDays((int) settings[1]);
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

    private void showDeleteConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm_delete_title))
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(getString(R.string.confirming), (dialog, which) -> {
                    // Elimina todos los fichajes del usuario actual
                    String username = dbHelper.getCurrentUsername(this);
                    dbHelper.deleteAllFichajes(username);
                    Toast.makeText(this, getString(R.string.history_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                    // Cancelar si el usuario no acepta
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
}