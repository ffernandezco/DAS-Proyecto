package eus.ehu.dasproyecto.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import eus.ehu.dasproyecto.data.Fichaje;
import eus.ehu.dasproyecto.data.FichajeDatabase;

import android.util.Log;

public class FichajeViewModel extends AndroidViewModel {
    private final LiveData<List<Fichaje>> fichajes;

    public FichajeViewModel(Application application) {
        super(application);
        FichajeDatabase db = FichajeDatabase.getInstance(application);
        fichajes = db.fichajeDao().obtenerTodosLosFichajes();

        fichajes.observeForever(lista -> {
            Log.d("FichajeViewModel", "Fichajes en la BD: " + lista.size());
        });
    }

    public LiveData<List<Fichaje>> getFichajes() {
        return fichajes;
    }

    public void insertarFichaje(Fichaje fichaje) {
        new Thread(() -> FichajeDatabase.getInstance(getApplication())
                .fichajeDao()
                .insertarFichaje(fichaje)).start();
    }
}
