#!/bin/bash
# ──────────────────────────────────────────────────────────────────────────────
# kafka-create-topics.sh
# Run this ONCE after Kafka is healthy to pre-provision all required topics.
# With KAFKA_AUTO_CREATE_TOPICS_ENABLE=false, topics must exist before producers
# and consumers start. This script creates them idempotently.
# ──────────────────────────────────────────────────────────────────────────────

KAFKA_BROKER="${KAFKA_BROKER:-kafka:9092}"
REPLICATION="${REPLICATION_FACTOR:-1}"   # Use 3 for prod cluster
PARTITIONS="${PARTITIONS:-3}"            # 1 partition per service is fine for local dev

echo "Provisioning Kafka topics on broker: $KAFKA_BROKER"

kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic transactions.created \
  --replication-factor "$REPLICATION" \
  --partitions "$PARTITIONS"

kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic risk.scored \
  --replication-factor "$REPLICATION" \
  --partitions "$PARTITIONS"

kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic fraud.decision.made \
  --replication-factor "$REPLICATION" \
  --partitions "$PARTITIONS"

# Dead Letter Queues — for failed message processing
kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic transactions.created.DLT \
  --replication-factor "$REPLICATION" \
  --partitions 1

kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic risk.scored.DLT \
  --replication-factor "$REPLICATION" \
  --partitions 1

kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic fraud.decision.made.DLT \
  --replication-factor "$REPLICATION" \
  --partitions 1

echo "All Kafka topics provisioned successfully."
kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --list
