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
| `INTERNAL_TOKEN` | Token compartido con el BFF (`X-Internal-Token`). Sin él, `/api/mq/**` responde **401**. |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers de Kafka para `audit.timeline`. En local, `localhost:9092`. |

Los mismos datos de Rabbit están en `barriodigital-infra/mq/.env`. Este servicio no ve el JWT: lo llama el BFF. Orden de seguridad: **401** token interno → **403** rol Admin.

## Auditoría en audit.timeline (EP3-27)

Tras crear o eliminar colas, exchanges o bindings, o vaciar una cola, mq-admin encola un evento en `audit.timeline` (`queue|exchange|binding` + `created|deleted|purged`). El productor usa `acks=all` e idempotencia, un hilo y cola de 1000. Actor desde `X-User-Id|Name|Roles` y `X-Source-Ip`.

`RabbitAdminService` no importa clases de Kafka: publican `QueueHttpService`, `ExchangeHttpService` y `BindingHttpService`.

Si Kafka falla o el servicio se apaga con eventos pendientes, queda `ERROR evento-auditoria-no-publicado` con el JSON. Un reinicio controlado (`docker compose restart`) no pierde esos eventos: se reconstruyen desde el log. Sin `NewTopic`; `spring.kafka.admin.auto-create=false`. El health de Kafka está desactivado.
