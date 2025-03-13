package eus.ehu.dasproyecto;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.List;

public class WorkTimeCheckWorker extends Worker {
    private final DatabaseHelper dbHelper;
    private final NotificationHelper notificationHelper;

    public WorkTimeCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        dbHelper = new DatabaseHelper(context);
        notificationHelper = new NotificationHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Revisar si se debería o no enviar notificación
        if (!notificationHelper.shouldSendNotification(getApplicationContext())) {
            return Result.success();
        }

        List<Fichaje> todaysFichajes = dbHelper.obtenerFichajesDeHoy();
        float[] settings = dbHelper.getSettings();
        float weeklyHours = settings[0];
        int workingDays = (int) settings[1];

        float dailyHours = WorkTimeCalculator.calculateDailyHours(weeklyHours, workingDays);
        long[] timeWorked = WorkTimeCalculator.getTimeWorkedToday(todaysFichajes);
        long[] timeRemaining = WorkTimeCalculator.getRemainingTime(timeWorked, dailyHours);

        boolean isClockedIn = WorkTimeCalculator.isCurrentlyClockedIn(todaysFichajes);

        // Solo enviar notificación si hay fichaje activo y se alcanzan las horas
        if ((timeRemaining[2] == 1 || (timeRemaining[0] == 0 && timeRemaining[1] == 0)) && isClockedIn) {
            notificationHelper.sendWorkCompleteNotification();
            notificationHelper.recordNotificationSent(getApplicationContext());
        }

        return Result.success();
    }
}
