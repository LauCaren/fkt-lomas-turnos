package ar.edu.uade.recordatorios.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Job periódico simple con ScheduledExecutorService. Para producción con
 * necesidades de persistencia de jobs y recuperación ante caídas del
 * proceso, reemplazar por Quartz Scheduler.
 */
public class SchedulerService {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void iniciar(Runnable tarea, long intervaloMinutos) {
        executor.scheduleAtFixedRate(tarea, 0, intervaloMinutos, TimeUnit.MINUTES);
    }

    public void detener() {
        executor.shutdown();
    }
}
