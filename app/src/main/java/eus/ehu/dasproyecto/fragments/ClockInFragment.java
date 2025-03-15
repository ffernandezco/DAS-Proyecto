package eus.ehu.dasproyecto.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eus.ehu.dasproyecto.DatabaseHelper;
import eus.ehu.dasproyecto.Fichaje;
import eus.ehu.dasproyecto.NotificationHelper;
import eus.ehu.dasproyecto.R;
import eus.ehu.dasproyecto.WorkTimeCalculator;

public class ClockInFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private TextView tvEstadoFichaje;
    private TextView tvTimeWorked;
    private TextView tvTimeRemaining;
    private Button btnFichar;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private NotificationHelper notificationHelper;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clock_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        notificationHelper = new NotificationHelper(requireContext());

        tvEstadoFichaje = view.findViewById(R.id.tvEstadoFichaje);
        tvTimeWorked = view.findViewById(R.id.tvTimeWorked);
        tvTimeRemaining = view.findViewById(R.id.tvTimeRemaining);
        btnFichar = view.findViewById(R.id.btnFichar);

        btnFichar.setOnClickListener(v -> checkLocationPermissionAndRegister());

        actualizarEstadoUI();

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                actualizarEstadoUI();
                checkWorkTimeCompleted();
                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarEstadoUI();
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void checkLocationPermissionAndRegister() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocationAndRegister();
        }
    }

    private void getCurrentLocationAndRegister() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
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

        Fichaje ultimoFichaje = dbHelper.obtenerUltimoFichajeDelDia(fechaActual);

        if (ultimoFichaje == null || ultimoFichaje.horaSalida != null) {
            Fichaje nuevoFichaje = new Fichaje(fechaActual, horaActual, null, latitude, longitude);
            dbHelper.insertarFichaje(nuevoFichaje);
        } else {
            ultimoFichaje.horaSalida = horaActual;
            ultimoFichaje.latitud = latitude;
            ultimoFichaje.longitud = longitude;
            dbHelper.actualizarFichaje(ultimoFichaje);
        }

        actualizarEstadoUI();
    }

    private void actualizarEstadoUI() {
        List<Fichaje> todaysFichajes = dbHelper.obtenerFichajesDeHoy();
        float[] settings = dbHelper.getSettings();
        float weeklyHours = settings[0];
        int workingDays = (int) settings[1];

        float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);
        long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
        long[] timeRemaining = WorkTimeCalculator.getRemainingTime(timeWorked, dailyHours);

        String timeWorkedStr = WorkTimeCalculator.formatTime(timeWorked[0], timeWorked[1]);
        String timeRemainingStr = WorkTimeCalculator.formatTime(timeRemaining[0], timeRemaining[1]);

        boolean isClockedIn = WorkTimeCalculator.isCurrentlyClockedIn(todaysFichajes);

        if (isClockedIn) {
            String horaFichaje = WorkTimeCalculator.getLastClockInTime(todaysFichajes);
            tvEstadoFichaje.setText(getString(R.string.estado_fichado, horaFichaje));
            btnFichar.setText(getString(R.string.fichar_salida));
        } else {
            tvEstadoFichaje.setText(getString(R.string.estado_no_fichado));
            btnFichar.setText(getString(R.string.fichar_entrada));
        }

        tvTimeWorked.setText(getString(R.string.time_worked, timeWorkedStr));

        int color = timeRemaining[2] == 0 ?
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark) :
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark);

        tvTimeRemaining.setText(timeRemaining[2] == 0 ?
                getString(R.string.time_remaining, timeRemainingStr) :
                getString(R.string.overtime, timeRemainingStr));
        tvTimeRemaining.setTextColor(color);
    }

    private void checkWorkTimeCompleted() {
        if (!notificationHelper.shouldSendNotification(requireContext())) {
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
            notificationHelper.recordNotificationSent(requireContext());
        }
    }
}
