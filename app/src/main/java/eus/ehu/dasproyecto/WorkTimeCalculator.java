package eus.ehu.dasproyecto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WorkTimeCalculator {

    // Calcula los minutos fichados en la fecha actual
    public static long getMinutesWorkedToday(List<Fichaje> todaysFichajes) {
        long totalMinutes = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        for (Fichaje fichaje : todaysFichajes) {
            if (fichaje.horaEntrada != null && fichaje.horaSalida != null) {
                try {
                    Date entrada = sdf.parse(fichaje.horaEntrada);
                    Date salida = sdf.parse(fichaje.horaSalida);

                    long diffInMillis = salida.getTime() - entrada.getTime();
                    long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

                    totalMinutes += diffInMinutes;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        // Suma el tiempo de los fichajes sin finalizar (en curso)
        Fichaje activeFichaje = getActiveFichaje(todaysFichajes);
        if (activeFichaje != null) {
            try {
                Date entrada = sdf.parse(activeFichaje.horaEntrada);
                Date now = sdf.parse(sdf.format(new Date())); // Current time

                long diffInMillis = now.getTime() - entrada.getTime();
                long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

                totalMinutes += diffInMinutes;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return totalMinutes;
    }

    // Adaptar a hh:mm para visualizar por pantalla
    public static String formatMinutes(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hours, mins);
    }

    // Horas por semana / días que se trabajan
    public static float calculateDailyHours(float weeklyHours, int workingDays) {
        if (workingDays <= 0) return 0;
        return weeklyHours / workingDays;
    }

    // Cálculo de los minutos restantes
    public static long getRemainingMinutes(long minutesWorked, float dailyHours) {
        long dailyMinutes = (long)(dailyHours * 60);
        long remaining = dailyMinutes - minutesWorked;
        return Math.max(0, remaining); // Control de negativos
    }

    public static String getLastClockInTime(List<Fichaje> fichajes) {
        for (int i = fichajes.size() - 1; i >= 0; i--) {
            if (fichajes.get(i).horaSalida == null) {
                return fichajes.get(i).horaEntrada;
            }
        }
        return "N/A"; // Si no hay fichaje activo
    }

    // Auxiliar para fichaje activo
    public static Fichaje getActiveFichaje(List<Fichaje> todaysFichajes) {
        for (Fichaje fichaje : todaysFichajes) {
            if (fichaje.horaEntrada != null && fichaje.horaSalida == null) {
                return fichaje;
            }
        }
        return null;
    }

    // Auxiliar para saber si hay un fichaje activo
    public static boolean isCurrentlyClockedIn(List<Fichaje> todaysFichajes) {
        return getActiveFichaje(todaysFichajes) != null;
    }
}
