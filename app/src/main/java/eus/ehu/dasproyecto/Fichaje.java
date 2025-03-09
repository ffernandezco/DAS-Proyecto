package eus.ehu.dasproyecto;

public class Fichaje {
    public int id;
    public String fecha;
    public String horaEntrada;
    public String horaSalida;
    public double latitud;
    public double longitud;

    public Fichaje(int id, String fecha, String horaEntrada, String horaSalida, double latitud, double longitud) {
        this.id = id;
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Fichaje(String fecha, String horaEntrada, String horaSalida, double latitud, double longitud) {
        this(-1, fecha, horaEntrada, horaSalida, latitud, longitud);
    }
}
