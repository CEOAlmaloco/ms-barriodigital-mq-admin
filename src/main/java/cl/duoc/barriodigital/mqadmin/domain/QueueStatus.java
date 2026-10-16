package cl.duoc.barriodigital.mqadmin.domain;

public record QueueStatus(String name, int messageCount, int consumerCount) {
}
