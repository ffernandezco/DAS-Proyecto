package eus.ehu.dasproyecto.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FichajeDao {
    @Insert
    void insertarFichaje(Fichaje fichaje);

    @Query("SELECT * FROM fichajes ORDER BY fecha DESC")
    LiveData<List<Fichaje>> obtenerTodosLosFichajes();

    @Query("SELECT * FROM fichajes WHERE fecha = :fecha ORDER BY horaEntrada DESC LIMIT 1")
    Fichaje obtenerUltimoFichajeDelDia(String fecha);

    @Update
    void actualizarFichaje(Fichaje fichaje);
}
