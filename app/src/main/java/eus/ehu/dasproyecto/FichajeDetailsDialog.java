package eus.ehu.dasproyecto;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class FichajeDetailsDialog extends DialogFragment {

    private Fichaje fichaje;

    public FichajeDetailsDialog(Fichaje fichaje) {
        this.fichaje = fichaje;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fichaje_details, container, false);

        TextView tvFecha = view.findViewById(R.id.tvDetailFecha);
        TextView tvEntrada = view.findViewById(R.id.tvDetailEntrada);
        TextView tvSalida = view.findViewById(R.id.tvDetailSalida);
        TextView tvLocation = view.findViewById(R.id.tvDetailLocation);
        Button btnVerMapa = view.findViewById(R.id.btnVerMapa);
        Button btnCerrar = view.findViewById(R.id.btnCerrar);

        tvFecha.setText("Fecha: " + fichaje.fecha);
        tvEntrada.setText("Hora entrada: " + fichaje.horaEntrada);

        String salida = fichaje.horaSalida != null ? fichaje.horaSalida : "Pendiente";
        tvSalida.setText("Hora salida: " + salida);

        if (fichaje.latitud == 0.0 && fichaje.longitud == 0.0) {
            tvLocation.setText("Ubicación: No disponible");
            btnVerMapa.setEnabled(false);
        } else {
            tvLocation.setText(String.format("Ubicación: %.6f, %.6f",
                    fichaje.latitud, fichaje.longitud));
            btnVerMapa.setEnabled(true);
        }

        btnVerMapa.setOnClickListener(v -> {
            if (fichaje.latitud != 0.0 || fichaje.longitud != 0.0) {
                String uri = "geo:" + fichaje.latitud + "," + fichaje.longitud + "?q=" +
                        fichaje.latitud + "," + fichaje.longitud + "(Ubicación de fichaje)";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });

        btnCerrar.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}