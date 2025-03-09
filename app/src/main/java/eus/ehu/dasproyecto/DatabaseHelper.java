package eus.ehu.dasproyecto;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "fichaje_db";
    private static final int DATABASE_VERSION = 1;

    //Inicializar tabla de fichajes

    private static final String TABLE_FICHAJES = "fichajes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FECHA = "fecha";
    private static final String COLUMN_HORA_ENTRADA = "hora_entrada";
    private static final String COLUMN_HORA_SALIDA = "hora_salida";
    private static final String COLUMN_LATITUD = "latitud";
    private static final String COLUMN_LONGITUD = "longitud";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_FICHAJES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_FECHA + " TEXT, " +
                COLUMN_HORA_ENTRADA + " TEXT, " +
                COLUMN_HORA_SALIDA + " TEXT, " +
                COLUMN_LATITUD + " REAL, " +
                COLUMN_LONGITUD + " REAL)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FICHAJES);
        onCreate(db);
    }

    public void insertarFichaje(Fichaje fichaje) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FECHA, fichaje.fecha);
        values.put(COLUMN_HORA_ENTRADA, fichaje.horaEntrada);
        values.put(COLUMN_HORA_SALIDA, fichaje.horaSalida);
        values.put(COLUMN_LATITUD, fichaje.latitud);
        values.put(COLUMN_LONGITUD, fichaje.longitud);

        db.insert(TABLE_FICHAJES, null, values);
        db.close();
    }

    // Devuelve el listado completo, e.g. RecyclerView
    public List<Fichaje> obtenerTodosLosFichajes() {
        List<Fichaje> listaFichajes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FICHAJES + " ORDER BY fecha DESC, hora_entrada DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Fichaje fichaje = new Fichaje(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getDouble(4),
                        cursor.getDouble(5)
                );
                listaFichajes.add(fichaje);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return listaFichajes;
    }

    // Devuelve el último fichaje para poderlo actualizar (trampa con limit)
    public Fichaje obtenerUltimoFichajeDelDia(String fecha) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FICHAJES + " WHERE fecha = ? ORDER BY hora_entrada DESC LIMIT 1",
                new String[]{fecha});

        if (cursor.moveToFirst()) {
            Fichaje fichaje = new Fichaje(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getDouble(4),
                    cursor.getDouble(5)
            );
            cursor.close();
            db.close();
            return fichaje;
        }
        cursor.close();
        db.close();
        return null;
    }

    //Añade la hora de salida y la ubicación
    public void actualizarFichaje(Fichaje fichaje) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HORA_SALIDA, fichaje.horaSalida);
        values.put(COLUMN_LATITUD, fichaje.latitud);
        values.put(COLUMN_LONGITUD, fichaje.longitud);

        db.update(TABLE_FICHAJES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(fichaje.id)});
        db.close();
    }
}