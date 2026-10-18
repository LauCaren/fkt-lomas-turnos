package com.fktlomas.turnos;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableAsync
@EnableScheduling
// Las tareas programadas (recordatorios, resúmenes, retención) toman un lock en la base antes de
// correr: si algún día hay más de una instancia del backend, cada tarea corre en UNA sola y nadie
// recibe el mismo recordatorio dos veces. Ver la tabla shedlock (V32) y @SchedulerLock en cada job.
// defaultLockAtMostFor es el tope por si una instancia muere en medio de una tarea.
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
@SpringBootApplication
public class TurnosApplication {

    /**
     * El centro funciona en hora argentina, y todo el código razona en esa hora: "hoy" en la
     * agenda (LocalDate.now()), si un turno ya pasó, a qué hora sale el resumen del lunes. Los
     * servidores en la nube corren casi siempre en UTC: sin esto, a partir de las 21 h de
     * Argentina el backend creía que ya era el día siguiente. Se fija acá, antes de que arranque
     * Spring, para que valga en cualquier host sin depender de que alguien configure la variable
     * TZ. Se puede pisar con APP_TIMEZONE si algún día hiciera falta.
     */
    static final String ZONA_HORARIA_POR_DEFECTO = "America/Argentina/Buenos_Aires";

    public static void main(String[] args) {
        String zona = System.getenv().getOrDefault("APP_TIMEZONE", ZONA_HORARIA_POR_DEFECTO);
        TimeZone.setDefault(TimeZone.getTimeZone(zona));
        SpringApplication.run(TurnosApplication.class, args);
    }
}
