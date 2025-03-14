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

    // Day selectors
    private ToggleButton toggleMonday, toggleTuesday, toggleWednesday,
            toggleThursday, toggleFriday, toggleSaturday, toggleSunday;
    private List<ToggleButton> dayToggles;

    // To store selected hours and minutes
    private int selectedHours = 40;
    private int selectedMinutes = 0;

    // Constants for the pickers
    private static final int MAX_HOURS = 168; // Maximum of 168 hours in a week
    private static final int MIN_HOURS = 0;
    private static final int MAX_MINUTES = 55; // In 5-minute increments
    private static final int MIN_MINUTES = 0;
    private static final int MINUTE_INCREMENT = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        // Initialize UI components
        spinnerLanguage = findViewById(R.id.spinnerLanguage);

        // Initialize number picker components
        tvHoursValue = findViewById(R.id.tvHoursValue);
        tvMinutesValue = findViewById(R.id.tvMinutesValue);
        btnIncreaseHours = findViewById(R.id.btnIncreaseHours);
        btnDecreaseHours = findViewById(R.id.btnDecreaseHours);
        btnIncreaseMinutes = findViewById(R.id.btnIncreaseMinutes);
        btnDecreaseMinutes = findViewById(R.id.btnDecreaseMinutes);

        btnDeleteHistory = findViewById(R.id.btnDeleteHistory);

        // Set up delete history button
        btnDeleteHistory.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        btnSave = findViewById(R.id.btnSaveSettings);

        // Initialize day toggles
        initializeDayToggles();

        // Set up language spinner
        String[] languages = {"Español", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);

        // Load saved settings
        loadSavedSettings();

        // Set up picker buttons
        setupNumberPickers();

        // Save button click listener
        btnSave.setOnClickListener(v -> {
            String selectedLanguage = spinnerLanguage.getSelectedItemPosition() == 0 ? "es" : "en";

            // Calculate weekly hours from selected hours and minutes
            float weeklyHours = selectedHours + (selectedMinutes / 60.0f);

            // Count selected working days
            int workingDays = countSelectedDays();

            if (weeklyHours <= 0 || weeklyHours > 168 || workingDays <= 0 || workingDays > 7) {
                Toast.makeText(this, getString(R.string.invalid_settings),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Save settings
            saveLanguagePreference(selectedLanguage);
            dbHelper.saveSettings(weeklyHours, workingDays);

            // Update locale and restart app
            setAppLocale(selectedLanguage);
            restartApp();
        });
    }

    private void setupNumberPickers() {
        // Hours controls
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

        // Minutes controls
        btnIncreaseMinutes.setOnClickListener(v -> {
            selectedMinutes += MINUTE_INCREMENT;
            if (selectedMinutes > MAX_MINUTES) {
                selectedMinutes = MIN_MINUTES;
                // Increment hour if minutes wrap around
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
                // Decrement hour if minutes wrap around
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

        // Add to list for easier handling
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
        // Default to Monday-Friday if the number doesn't make sense
        if (days <= 0 || days > 7) {
            days = 5;
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < 5); // Check Mon-Fri
            }
            return;
        }

        // Otherwise, try to set the most common work pattern
        if (days == 5) {
            // Standard Mon-Fri
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < 5);
            }
        } else if (days < 5) {
            // First days of the week
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < days);
            }
        } else {
            // More than 5 days, add weekend days
            for (int i = 0; i < dayToggles.size(); i++) {
                dayToggles.get(i).setChecked(i < days);
            }
        }
    }

    private void loadSavedSettings() {
        // Load language preference
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        spinnerLanguage.setSelection(lang.equals("es") ? 0 : 1);

        // Load hours and days settings
        float[] settings = dbHelper.getSettings();

        // Split hours into whole hours and minutes
        selectedHours = (int) settings[0];
        // Round to the nearest 5 minutes
        selectedMinutes = Math.round((settings[0] - selectedHours) * 60 / MINUTE_INCREMENT) * MINUTE_INCREMENT;

        updateHoursDisplay();
        updateMinutesDisplay();

        // Set working days
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
                    // Elimina todos los fichakes
                    dbHelper.deleteAllFichajes();
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