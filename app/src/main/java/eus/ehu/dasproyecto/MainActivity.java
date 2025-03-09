package eus.ehu.dasproyecto;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private FichajeAdapter adapter;
    private TextView tvEstadoFichaje;
    private Button btnFichar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Initialize UI components
        tvEstadoFichaje = findViewById(R.id.tvEstadoFichaje);
        btnFichar = findViewById(R.id.btnFichar);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FichajeAdapter();
        recyclerView.setAdapter(adapter);

        // Set the click listener for the button
        btnFichar.setOnClickListener(v -> registrarFichaje());

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

    private void registrarFichaje() {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        String fechaActual = sdfFecha.format(new Date());
        String horaActual = sdfHora.format(new Date());

        // Buscar el último fichaje del día actual
        Fichaje ultimoFichaje = dbHelper.obtenerUltimoFichajeDelDia(fechaActual);

        if (ultimoFichaje == null || ultimoFichaje.horaSalida != null) {
            // Si no hay fichaje del día o ya se fichó la salida, registrar nueva entrada
            Fichaje nuevoFichaje = new Fichaje(fechaActual, horaActual, null, 0.0, 0.0);
            dbHelper.insertarFichaje(nuevoFichaje);
        } else {
            // Si ya hay fichaje de entrada sin salida, actualizar con la hora de salida
            ultimoFichaje.horaSalida = horaActual;
            dbHelper.actualizarFichaje(ultimoFichaje);
        }

        // Update UI state and list after registering a clock in/out
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
}