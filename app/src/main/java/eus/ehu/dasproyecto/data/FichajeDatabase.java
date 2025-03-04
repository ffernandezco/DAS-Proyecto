package eus.ehu.dasproyecto.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Fichaje.class}, version = 1, exportSchema = false)
public abstract class FichajeDatabase extends RoomDatabase {
    private static volatile FichajeDatabase INSTANCE;

    public abstract FichajeDao fichajeDao();

    public static FichajeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FichajeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    FichajeDatabase.class, "fichaje_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
