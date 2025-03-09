package eus.ehu.dasproyecto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private FichajeAdapter adapter;
    private TextView tvEstadoFichaje;
    private Button btnFichar;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize UI components
        tvEstadoFichaje = findViewById(R.id.tvEstadoFichaje);
        btnFichar = findViewById(R.id.btnFichar);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FichajeAdapter(fichaje -> showFichajeDetails(fichaje));
        recyclerView.setAdapter(adapter);

        // Set the click listener for the button
        btnFichar.setOnClickListener(v -> checkLocationPermissionAndRegister());

        // Initial update of the UI and list
        actualizarEstadoUI();
        actualizarLista();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update UI state when returning to the activity
        actualizarEstadoUI();
        actualizarLista();
    }

    private void checkLocationPermissionAndRegister() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocationAndRegister();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndRegister();
            } else {
                // Registro sin ubicación si no está disponible, se avisa con Toast
                Toast.makeText(this, "No se ha podido obtener la ubicación. Se registrará el fichaje sin ella, pero conviene que revises los permisos otorgados en la configuración.",
                        Toast.LENGTH_LONG).show();
                registrarFichaje(0.0, 0.0);
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

        Fichaje ultimoFichaje = dbHelper.obtenerUltimoFichajeDelDia(fechaActual);

        if (ultimoFichaje == null || ultimoFichaje.horaSalida != null) {
            // No hay fichaje o el último fichaje ya tiene hora de salida (no fichado)
            tvEstadoFichaje.setText("Estado: No fichado");
            btnFichar.setText("Fichar Entrada");
        } else {
            // Hay fichaje de entrada sin salida (fichado)
            tvEstadoFichaje.setText("Estado: Fichado desde " + ultimoFichaje.horaEntrada);
            btnFichar.setText("Fichar Salida");
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