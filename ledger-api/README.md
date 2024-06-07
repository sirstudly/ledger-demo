# Bank Ledger Public API

The following application handles the REST API for the Bank Ledger Demo.

## Workflow Overview

1. **API Request**: The client sends an API request to perform an operation.
2. **Publish Event**: The API application publishes an appropriate event to Kafka.
3. **Wait for Completion**:
    - The API application waits for a short period (e.g., 2000ms) by listening to the Kafka completion event.
    - It uses a `CompletableFuture` to track the completion of the request.
4. **API Response**:
    - If the request is invalid or fails some business validation, a "failed" status is returned to the client.
      - **FIXME: handle DB errors (eg. duplicate keys) by sending a FAILED event??**
    - If the request completes within the timeout, the application returns the successful result to the client.
    - If the timeout is reached, it returns a "pending" status to the client.
    - Once the result is ready, the client can check back at a later time.

## Implementation Summary

### Key Components

1. **Controller**: Handles incoming API requests and publishes events to Kafka.
2. **Service**: Manages the waiting for account creation completion and handles the completion of the `CompletableFuture`.
3. **Kafka Consumer**: Listens to the `account-completion-events` topic to complete the account creation process.


## Build Instructions and API Documentation

The Swagger API documentation will give the full description of all the API endpoints.
You can setup the Swagger documentation by running the following: ****TODO****


## TODO:
Use JWT for authentication/authorisation?