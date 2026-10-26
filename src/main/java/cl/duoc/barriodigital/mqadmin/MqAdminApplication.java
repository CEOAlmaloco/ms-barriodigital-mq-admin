package cl.duoc.barriodigital.mqadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Arranca mq-admin.
 * Deja listo el proceso que lista, crea y borra colas, exchanges y bindings.
 * Escucha en el puerto 8084, que no es público.
 */
@SpringBootApplication
public class MqAdminApplication {

    /**
     * Punto de entrada. Spring Boot inicia el proceso y abre el puerto 8084.
     */
    public static void main(String[] args) {
        SpringApplication.run(MqAdminApplication.class, args);
    }
}
