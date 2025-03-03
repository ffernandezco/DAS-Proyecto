package eus.ehu.dasproyecto.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FichajeDao {
    @Insert
    void insertarFichaje(Fichaje fichaje);

    @Query("SELECT * FROM fichajes ORDER BY fecha DESC")
    LiveData<List<Fichaje>> obtenerTodosLosFichajes();
}
