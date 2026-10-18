# ms-barriodigital-mq-admin

Administración de RabbitMQ de **BarrioDigital**.

Puerto: **8084**

## Arranque

El broker tiene que estar levantado (`mq/compose.yml` en `barriodigital-infra`). Este servicio no lo incluye.

```powershell
copy .env.example .env
.\mvnw.cmd spring-boot:run
```

```powershell
curl http://localhost:8084/actuator/health
```

Con el broker prendido y las credenciales correctas, la respuesta es `{"status":"UP"}`.

## Variables (`.env`, no subir a git)

| Variable | Qué va |
|---|---|
| `RABBITMQ_HOST` | Host del broker. En local, `localhost`. |
| `RABBITMQ_PORT` | Puerto AMQP. En local, `5672`. |
| `RABBITMQ_MANAGEMENT_URL` | HTTP API del Management. En local, `http://127.0.0.1:15672`. El listado de colas sale de ahí. |
| `RABBITMQ_USER` | Usuario de RabbitMQ. |
| `RABBITMQ_PASSWORD` | Password. Si tiene `$`, en el `.env` va como `$$`. |

Los mismos datos están en `barriodigital-infra/mq/.env`. Este servicio no ve el JWT: lo llama el BFF.
