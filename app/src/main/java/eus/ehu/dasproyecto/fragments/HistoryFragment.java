package eus.ehu.dasproyecto.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import eus.ehu.dasproyecto.DatabaseHelper;
import eus.ehu.dasproyecto.Fichaje;
import eus.ehu.dasproyecto.FichajeAdapter;
import eus.ehu.dasproyecto.FichajeDetailsDialog;
import eus.ehu.dasproyecto.R;

public class HistoryFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private FichajeAdapter adapter;
    private TextView tvEmptyHistory;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        // Iniciar RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FichajeAdapter(fichaje -> showFichajeDetails(fichaje));
        recyclerView.setAdapter(adapter);

        actualizarLista();
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarLista();
    }

    private void actualizarLista() {
        List<Fichaje> fichajes = dbHelper.obtenerTodosLosFichajes();
        adapter.setFichajes(fichajes);

        // En caso de no haber fichajes, no mostrar nada
        if (fichajes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyHistory.setVisibility(View.GONE);
        }
    }

    private void showFichajeDetails(Fichaje fichaje) {
        FichajeDetailsDialog dialog = new FichajeDetailsDialog(fichaje);
        dialog.show(getParentFragmentManager(), "FichajeDetailsDialog");
    }
}