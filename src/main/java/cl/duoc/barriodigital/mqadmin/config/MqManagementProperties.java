package cl.duoc.barriodigital.mqadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * URL, usuario, clave y vhost de la API HTTP de Management.
 * El listado sale de esa API, porque AMQP no publica el inventario del vhost.
 */
@Component
@ConfigurationProperties(prefix = "barriodigital.mq.management")
public class MqManagementProperties {

    private String url = "http://127.0.0.1:15672";
    private String username = "guest";
    private String password = "guest";
    private String vhost = "/";

    /** URL del Management, por ejemplo http://127.0.0.1:15672. */
    public String getUrl() {
        return url;
    }

    /** Spring asigna la URL leída del yml. */
    public void setUrl(String url) {
        this.url = url;
    }

    /** Usuario con el que se llama a la API HTTP de Management. */
    public String getUsername() {
        return username;
    }

    /** Spring asigna el usuario leído del yml. */
    public void setUsername(String username) {
        this.username = username;
    }

    /** Clave del Management. No se incluye en las respuestas HTTP de mq-admin. */
    public String getPassword() {
        return password;
    }

    /** Spring asigna la clave leída del yml. */
    public void setPassword(String password) {
        this.password = password;
    }

    /** Vhost de RabbitMQ. En local suele ser /. */
    public String getVhost() {
        return vhost;
    }

    /** Spring asigna el vhost leído del yml. */
    public void setVhost(String vhost) {
        this.vhost = vhost;
    }
}
