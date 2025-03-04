package eus.ehu.dasproyecto.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import eus.ehu.dasproyecto.R;
import eus.ehu.dasproyecto.data.Fichaje;
import eus.ehu.dasproyecto.data.FichajeDao;
import eus.ehu.dasproyecto.data.FichajeDatabase;
import eus.ehu.dasproyecto.viewmodel.FichajeViewModel;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private FichajeViewModel fichajeViewModel;
    private FichajeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FichajeAdapter();
        recyclerView.setAdapter(adapter);

        fichajeViewModel = new ViewModelProvider(this).get(FichajeViewModel.class);
        fichajeViewModel.getFichajes().observe(this, fichajes -> {
            adapter.setFichajes(fichajes);
            actualizarEstadoBoton(fichajes);
        });

        findViewById(R.id.btnFichar).setOnClickListener(v -> registrarFichaje());
    }

    /**
     * Método para actualizar el estado del botón y el TextView según el último fichaje del día.
     */
    private void actualizarEstadoBoton(java.util.List<Fichaje> fichajes) {
        String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        boolean entradaFichada = false;

        for (Fichaje f : fichajes) {
            if (f.fecha.equals(fechaHoy) && f.horaSalida.equals("--:--")) {
                entradaFichada = true;
                break;
            }
        }

        Button btnFichar = findViewById(R.id.btnFichar);
        TextView tvEstadoFichaje = findViewById(R.id.tvEstadoFichaje);

        if (entradaFichada) {
            btnFichar.setText("Fichar Salida");
            tvEstadoFichaje.setText("Estado: Entrada fichada");
        } else {
            btnFichar.setText("Fichar Entrada");
            tvEstadoFichaje.setText("Estado: No fichado o salida registrada");
        }
    }

    private void registrarFichaje() {
        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String horaActual = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        new Thread(() -> {
            FichajeDao dao = FichajeDatabase.getInstance(getApplication()).fichajeDao();
            Fichaje ultimoFichaje = dao.obtenerUltimoFichajeDelDia(fecha);

            if (ultimoFichaje == null || !ultimoFichaje.horaSalida.equals("--:--")) {
                // No hay fichaje abierto o el último ya tiene salida -> Crear un nuevo fichaje
                Fichaje nuevoFichaje = new Fichaje(fecha, horaActual, "--:--", 0, 0);
                dao.insertarFichaje(nuevoFichaje);
            } else {
                // Último fichaje está abierto -> Registrar salida
                ultimoFichaje.horaSalida = horaActual;
                dao.actualizarFichaje(ultimoFichaje);
            }
        }).start();
    }
}
