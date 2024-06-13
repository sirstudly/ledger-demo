# Ledger Posting System Design and Implementation

See [PROBLEM.md](PROBLEM.md) for complete task details.

## Acceptance Criteria
- **Integration and Unit Test Cases:** Tests should be clear, behavior-driven, and cover both unit and integration aspects.
- **Communication:** The system should support both synchronous (e.g., REST APIs) and asynchronous (e.g., messaging) communication.
- **Swagger Spec:** Provide API documentation using Swagger.
- **Readme Documentation:** Include any additional instructions or information necessary for reviewers.

## Problem Statement Breakdown
- **Scalable Ledger System:** Must handle heavy write loads efficiently.
- **Modularity and Scalability:** Use microservices, CQRS, and event sourcing.
- **Retrievable Ledger Balances:** Ability to get ledger balances for a specific timestamp.
- **API-friendly:** For asset transfers and account management.
- **Behavior-Driven Testing:** Comprehensive tests that describe system behavior.


## Implementation Summary
The system is split into two smaller microservices:
1. The public RESTful API
  - handles all client requests
  - validates requests for correctness
  - create Kafka events for any "updates"
  - queries datastore for any "reads"
2. The event processor
  - consumes all "update" Kafka events
  - updates datastore
  - create Kafka completion events
  - create Kafka failure events (AKA "dead-letter" queue for manual processing)

Apache Kafka will be used as the event streaming platform to pass information between the two applications.
PostgreSQL is currently used as the datastore.
- future improvement: should we migrate to ksqlDB? https://developer.confluent.io/patterns/event-processing/event-processing-application/

## Benefits of This Approach

- **Decoupling**: The API and event processor applications are decoupled and communicate through Kafka, enhancing modularity and fault tolerance.
- **Scalability**: Both applications can scale independently based on their specific load characteristics.
- **Simplified Architecture**: Reduces the need for synchronous HTTP calls between the services, leveraging Kafka for reliable and efficient communication.
- **Consistency and Reliability**: Kafka ensures reliable delivery of events and can handle retries and failures gracefully.

## Build and Deployment

Run ```docker-compose up --build``` in this directory to build and run the following docker containers:
- ledger-common: build common Java package for use in ledger-api and event-processor
- ledger-api: The RESTful server implemented as a Spring Boot webapp. It can be accessed on port 6868. API endpoints are published in Swagger on http://localhost:6868/swagger-ui/index.html
- event-processor: Handles all updates to the datastore.
- Apache Kafka: Our event streaming platform. It can be accessed on port 29092.
- Apache ZooKeeper: The distribution and coordination server used by Kafka. It can be accessed on port 2181.
- PostgresSQL: Our datastore. It can be accessed on port 5432.

## Testing
The ``integration-tests`` folder contains end-to-end cucumber integration tests which you can run against the REST API
to validate correct behaviour. It is meant to run standalone and does not have any dependencies on the other projects.

## Data Model

TODO

## Sample Request/Response

TODO

attach Postman 

## Things still TODO
- move all repository classes to common
- Unit test coverage is not exhaustive. I've only included a few major classes as an example.
- No user authentication/authorization for any requests. Could use JWT to avoid hit to DB and to improve scalability.
- Setup a Jenkins server to run tests/redeploy via Git hooks.
- Get Ledger should return list of Accounts
  - Get Accounts should return list of transactions