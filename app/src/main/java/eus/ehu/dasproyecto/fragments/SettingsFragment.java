package eus.ehu.dasproyecto.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import eus.ehu.dasproyecto.DatabaseHelper;
import eus.ehu.dasproyecto.MainActivity;
import eus.ehu.dasproyecto.R;

public class SettingsFragment extends Fragment {
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLanguageFromPreferences();
    }

    private void applyLanguageFromPreferences() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        setAppLocale(lang);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        // Inicializar vistas
        initializeViews(view);

        // Configurar spinner de idioma
        String[] languages = {"Español", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);

        // Cargar configuración guardada
        loadSavedSettings();

        // Configurar botones de horas y minutos
        setupNumberPickers();

        // Listener para el botón de guardar
        btnSave.setOnClickListener(v -> saveSettings());

        // Listener para eliminar historial
        btnDeleteHistory.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void initializeViews(View view) {
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage);
        tvHoursValue = view.findViewById(R.id.tvHoursValue);
        tvMinutesValue = view.findViewById(R.id.tvMinutesValue);
        btnIncreaseHours = view.findViewById(R.id.btnIncreaseHours);
        btnDecreaseHours = view.findViewById(R.id.btnDecreaseHours);
        btnIncreaseMinutes = view.findViewById(R.id.btnIncreaseMinutes);
        btnDecreaseMinutes = view.findViewById(R.id.btnDecreaseMinutes);
        btnSave = view.findViewById(R.id.btnSaveSettings);
        btnDeleteHistory = view.findViewById(R.id.btnDeleteHistory);

        toggleMonday = view.findViewById(R.id.toggleMonday);
        toggleTuesday = view.findViewById(R.id.toggleTuesday);
        toggleWednesday = view.findViewById(R.id.toggleWednesday);
        toggleThursday = view.findViewById(R.id.toggleThursday);
        toggleFriday = view.findViewById(R.id.toggleFriday);
        toggleSaturday = view.findViewById(R.id.toggleSaturday);
        toggleSunday = view.findViewById(R.id.toggleSunday);

        dayToggles = new ArrayList<>();
        dayToggles.add(toggleMonday);
        dayToggles.add(toggleTuesday);
        dayToggles.add(toggleWednesday);
        dayToggles.add(toggleThursday);
        dayToggles.add(toggleFriday);
        dayToggles.add(toggleSaturday);
        dayToggles.add(toggleSunday);
    }

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

        btnIncreaseMinutes.setOnClickListener(v -> {
            selectedMinutes += MINUTE_INCREMENT;
            if (selectedMinutes > MAX_MINUTES) {
                selectedMinutes = MIN_MINUTES;
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

    private void loadSavedSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        spinnerLanguage.setSelection(lang.equals("es") ? 0 : 1);

        float[] settings = dbHelper.getSettings();

        selectedHours = (int) settings[0];
        selectedMinutes = Math.round((settings[0] - selectedHours) * 60 / MINUTE_INCREMENT) * MINUTE_INCREMENT;

        updateHoursDisplay();
        updateMinutesDisplay();
        setSelectedDays((int) settings[1]);
    }

    private void setSelectedDays(int days) {
        if (days <= 0 || days > 7) {
            days = 5;
        }
        for (int i = 0; i < dayToggles.size(); i++) {
            dayToggles.get(i).setChecked(i < days);
        }
    }

    private void saveSettings() {
        String selectedLanguage = spinnerLanguage.getSelectedItemPosition() == 0 ? "es" : "en";

        float weeklyHours = selectedHours + (selectedMinutes / 60.0f);
        int workingDays = countSelectedDays();

        if (weeklyHours <= 0 || weeklyHours > 168 || workingDays <= 0 || workingDays > 7) {
            Toast.makeText(requireContext(), getString(R.string.invalid_settings), Toast.LENGTH_SHORT).show();
            return;
        }

        saveLanguagePreference(selectedLanguage);
        dbHelper.saveSettings(weeklyHours, workingDays);
        setAppLocale(selectedLanguage);
        restartApp();
    }

    private int countSelectedDays() {
        int count = 0;
        for (ToggleButton toggle : dayToggles) {
            if (toggle.isChecked()) count++;
        }
        return count;
    }

    private void saveLanguagePreference(String lang) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", lang);
        editor.apply();
    }

    private void setAppLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = requireContext().getResources().getConfiguration();
        config.setLocale(locale);
        requireContext().getResources().updateConfiguration(config, requireContext().getResources().getDisplayMetrics());
    }

    private void restartApp() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(requireContext(), getString(R.string.settings_updated), Toast.LENGTH_LONG).show();
    }

    private void showDeleteConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete_title))
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(getString(R.string.confirming), (dialog, which) -> {
                    dbHelper.deleteAllFichajes();
                    Toast.makeText(requireContext(), getString(R.string.history_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
}
