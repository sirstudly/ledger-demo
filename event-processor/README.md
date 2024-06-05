# Bank Ledger Event Processor

This application processes Kafka events, performs the necessary business logic (mainly write to persistent store), and publishes completion events back to Kafka.

## Workflow Overview

1. **Event Processing**:
    - The application consumes Kafka events (eg. to update the data model).
    - It checks the database for previous existence and processes the creation.
    - Upon completion, it publishes completion event to Kafka.

## Implementation Summary

### Key Components

1. **Kafka Consumer**: Listens to the Kafka events to process creation/transfer events.
2. **Service**: Performs the business logic for account creation and publishes completion events to Kafka.
