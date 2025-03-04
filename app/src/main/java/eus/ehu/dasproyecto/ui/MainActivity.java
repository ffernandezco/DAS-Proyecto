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
import eus.ehu.dasproyecto.viewmodel.FichajeViewModel;

import android.util.Log;

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
        fichajeViewModel.getFichajes().observe(this, adapter::setFichajes);

        findViewById(R.id.btnFichar).setOnClickListener(v -> registrarFichaje());
    }

    private void registrarFichaje() {
        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        Fichaje fichaje = new Fichaje(fecha, hora, "--:--", 0, 0);
        fichajeViewModel.insertarFichaje(fichaje);

        fichajeViewModel.getFichajes().observe(this, fichajes -> {
            adapter.setFichajes(fichajes);
        });
    }
}
