package eus.ehu.dasproyecto.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "fichajes")
public class Fichaje {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String fecha;
    public String horaEntrada;
    public String horaSalida;
    public double latitud;
    public double longitud;

    public Fichaje(String fecha, String horaEntrada, String horaSalida, double latitud, double longitud) {
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.latitud = latitud;
        this.longitud = longitud;
    }
}
