package eus.ehu.dasproyecto;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private FichajeAdapter adapter;
    private TextView tvEstadoFichaje;
    private TextView tvTimeWorked;
    private TextView tvTimeRemaining;
    private Button btnFichar;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private NotificationHelper notificationHelper;
    private boolean notificationSent = false;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadSavedLanguage();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvEstadoFichaje = findViewById(R.id.tvEstadoFichaje);
        tvTimeWorked = findViewById(R.id.tvTimeWorked);
        tvTimeRemaining = findViewById(R.id.tvTimeRemaining);
        btnFichar = findViewById(R.id.btnFichar);

        notificationHelper = new NotificationHelper(this);
        checkAndResetNotificationFlag();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FichajeAdapter(fichaje -> showFichajeDetails(fichaje));
        recyclerView.setAdapter(adapter);

        btnFichar.setOnClickListener(v -> checkLocationPermissionAndRegister());

        // Solicitar permiso para enviar notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Solo necesario en las versiones modernas de Android
            requestNotificationPermission();
        }

        actualizarEstadoUI();
        actualizarLista();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                actualizarEstadoUI();
                checkWorkTimeCompleted(); // Comprobar el tiempo
                timerHandler.postDelayed(this, 1000); // Actualiza el estado cada segundo
            }
        };

        PeriodicWorkRequest workTimeCheckRequest =
                new PeriodicWorkRequest.Builder(WorkTimeCheckWorker.class,
                        1, TimeUnit.SECONDS)  // Comprobar cada segundo para notificaciones
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "periodicWorkTimeCheck",
                ExistingPeriodicWorkPolicy.REPLACE,
                workTimeCheckRequest);



        Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Calcular tiempo si se sale de la app
        actualizarEstadoUI();
        actualizarLista();

        timerHandler.postDelayed(timerRunnable, 1000); //Seguir actualizando cada minuto
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Como tenemos la hora exacta de fichaje, no es necesario consumir recursos
        // Se detiene el contador si se pausa la app y vuelve a comenzar a partir de la hora dada
        // en caso de que se vuelva a abrir usando onResume()
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void loadSavedLanguage() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lang = prefs.getString("language", "es");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void checkLocationPermissionAndRegister() {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaActual = sdfFecha.format(new Date());
        Fichaje ultimoFichaje = dbHelper.obtenerUltimoFichajeDelDia(fechaActual);

        if (ultimoFichaje != null && ultimoFichaje.horaSalida == null) {
            // Comprobar si el trabajador ha completado sus horas de trabajo
            List<Fichaje> todaysFichajes = dbHelper.obtenerFichajesDeHoy();
            float[] settings = dbHelper.getSettings();
            float weeklyHours = settings[0];
            int workingDays = (int) settings[1];

            float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);
            long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
            long dailyMinutesRequired = (long)(dailyHours * 60);

            // Mostrar dialog en caso de que haya trabajado menos de lo previsto
            if (timeWorked[0] < dailyMinutesRequired) {
                showClockOutConfirmationDialog();
                return;
            }
        }

        // Si se han completado horas diarias o es una acción de fichar
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocationAndRegister();
        }
    }

    //Dialog si se trata de salir antes de lo estipulado
    private void showClockOutConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm_clock_out_title))
                .setMessage(getString(R.string.confirm_clock_out_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    // Si acepta, se procede a cerrar el fichaje
                    getCurrentLocationAndRegister();
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                    // Si cancela, se cierra el dialog
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndRegister();
            } else {
                registrarFichaje(0.0, 0.0);
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Se muestra un toast en caso de que no se otorgue permiso para mostrar notificaciones
                Toast.makeText(this, getString(R.string.notification_permission_denied),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getCurrentLocationAndRegister() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        double latitude = 0.0;
                        double longitude = 0.0;

                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }

                        registrarFichaje(latitude, longitude);
                    }
                });
    }

    private void registrarFichaje(double latitude, double longitude) {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        String fechaActual = sdfFecha.format(new Date());
        String horaActual = sdfHora.format(new Date());

        // Buscar el último fichaje del día actual
        Fichaje ultimoFichaje = dbHelper.obtenerUltimoFichajeDelDia(fechaActual);

        if (ultimoFichaje == null || ultimoFichaje.horaSalida != null) {
            // Si no hay fichaje del día o ya se fichó la salida, registrar nueva entrada
            Fichaje nuevoFichaje = new Fichaje(fechaActual, horaActual, null, latitude, longitude);
            dbHelper.insertarFichaje(nuevoFichaje);
        } else {
            // Si ya hay fichaje de entrada sin salida, actualizar con la hora de salida
            ultimoFichaje.horaSalida = horaActual;
            ultimoFichaje.latitud = latitude; // Añadir ubicación
            ultimoFichaje.longitud = longitude;
            dbHelper.actualizarFichaje(ultimoFichaje);
        }

        // Actualizar estados
        actualizarEstadoUI();
        actualizarLista();
    }

    private void actualizarEstadoUI() {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaActual = sdfFecha.format(new Date());

        List<Fichaje> todaysFichajes = dbHelper.obtenerFichajesDeHoy();

        float[] settings = dbHelper.getSettings();
        float weeklyHours = settings[0];
        int workingDays = (int) settings[1];

        float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);

        // Calcular horas restantes y trabajadas en segundos
        long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
        long[] timeRemaining = WorkTimeCalculator.getRemainingTime(timeWorked, dailyHours);

        // Formatear tiempo
        String timeWorkedStr = WorkTimeCalculator.formatTime(timeWorked[0], timeWorked[1]);
        String timeRemainingStr = WorkTimeCalculator.formatTime(timeRemaining[0], timeRemaining[1]);

        // Actualizar UI
        boolean isClockedIn = WorkTimeCalculator.isCurrentlyClockedIn(todaysFichajes);

        // Textos a mostrar
        if (isClockedIn) {
            String horaFichaje = WorkTimeCalculator.getLastClockInTime(todaysFichajes);
            tvEstadoFichaje.setText(getString(R.string.estado_fichado, horaFichaje));
            btnFichar.setText(getString(R.string.fichar_salida));
        } else {
            tvEstadoFichaje.setText(getString(R.string.estado_no_fichado));
            btnFichar.setText(getString(R.string.fichar_entrada));
        }

        tvTimeWorked.setText(getString(R.string.time_worked, timeWorkedStr));

        // Comprobación horas trabajadas superiores a las de la jornada
        // (timeRemaining[2] == 1 implica que ya estamos en horas extra, se cambia a texto rojo)
        if (timeRemaining[2] == 0) {
            tvTimeRemaining.setText(getString(R.string.time_remaining, timeRemainingStr));
            tvTimeRemaining.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvTimeRemaining.setText(getString(R.string.overtime, timeRemainingStr));
            tvTimeRemaining.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void checkAndResetNotificationFlag() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lastNotificationDate = prefs.getString("last_notification_date", "");

        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaActual = sdfFecha.format(new Date());

        if (!fechaActual.equals(lastNotificationDate)) {
            // Resetear si se pasa al día siguiente
            notificationSent = false;
        }
    }

    private void checkWorkTimeCompleted() {
        if (!notificationHelper.shouldSendNotification(this)) {
            return; // No repetir notificaciones
        }

        List<Fichaje> todaysFichajes = dbHelper.obtenerFichajesDeHoy();
        float[] settings = dbHelper.getSettings();
        float weeklyHours = settings[0];
        int workingDays = (int) settings[1];

        float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);
        long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
        long[] timeRemaining = WorkTimeCalculator.getRemainingTime(timeWorked, dailyHours);

        boolean isClockedIn = WorkTimeCalculator.isCurrentlyClockedIn(todaysFichajes);

        // Enviar notificación si hay fichaje en curso y supera las horas
        if ((timeRemaining[2] == 1 || (timeRemaining[0] == 0 && timeRemaining[1] == 0)) && isClockedIn) {
            notificationHelper.sendWorkCompleteNotification();
            notificationHelper.recordNotificationSent(this);
        }
    }

    // Permisos para notificaciones
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Solo en versiones modernas
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void actualizarLista() {
        adapter.setFichajes(dbHelper.obtenerTodosLosFichajes());
    }

    private void showFichajeDetails(Fichaje fichaje) {
        FichajeDetailsDialog dialog = new FichajeDetailsDialog(fichaje);
        dialog.show(getSupportFragmentManager(), "FichajeDetailsDialog");
    }
}