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

![data model](https://www.moderntreasury.com/_next/image?url=https%3A%2F%2Fcdn.sanity.io%2Fimages%2F8nmbzj0x%2Fproduction%2F4498c4bf12ebec6822f9c3f4150a4e8254b7809b-2022x1002.png&w=3840&q=75)

## Sample Requests/Responses

The Swagger UI is temporarily deployed on https://7738-31-94-68-63.ngrok-free.app/swagger-ui/index.html
<br>However it currently doesn't support the "Try it out" feature due to a CORS error.

Here are some sample requests to get you started:

**Create Ledger:** `http://<host>:<port>/api/ledger`

Request:
```json
{
    "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfbd",
    "name": "My First Ledger",
    "description": "A collection of my accounts"
}
```

Response:
```json
{
    "status": "completed",
    "ledger": {
        "id": 1,
        "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfbd",
        "name": "My First Ledger",
        "description": "A collection of my accounts",
        "createdDate": "2024-06-13T23:07:22.170380786Z",
        "lastUpdatedDate": "2024-06-13T23:07:22.1704152Z"
    }
}
```

**Get Ledger:** `http://<host>:<port>/api/ledger/a42f714c-ce90-4a9c-a06e-9a1b8842dfbd`

Response:
```json
{
    "id": 1,
    "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfbd",
    "name": "My First Ledger",
    "description": "A collection of my accounts",
    "createdDate": "2024-06-09T01:34:54.746305Z",
    "lastUpdatedDate": "2024-06-09T01:34:54.746347Z"
}
```

**Create Ledger Account:** `http://<host>:<port>/api/ledger_account`

Request:
```json
{
    "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfac",
    "name": "My First Ledger Account",
    "description": "This is used to hold account transactions",
    "currency": "USD",
    "ledger": {
        "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfbd"
    }
}
```

Response:
```json
{
    "status": "completed",
    "ledgerAccount": {
        "id": 2,
        "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfac",
        "ledger": {
            "id": 1,
            "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfbd",
            "name": "My First Ledger",
            "description": "A collection of my accounts",
            "createdDate": "2024-06-13T23:07:22.170381Z",
            "lastUpdatedDate": "2024-06-13T23:07:22.170415Z"
        },
        "name": "My First Ledger Account",
        "description": "This is used to hold account transactions",
        "currency": "USD",
        "createdDate": "2024-06-13T23:07:29.549736445Z",
        "lastUpdatedDate": "2024-06-13T23:07:29.549750432Z"
    }
}
```

**Get Ledger Account:** `http://<host>:<port>/api/ledger_account/a42f714c-ce90-4a9c-a06e-9a1b8842dfac`

Response:
```json
{
    "id": 1,
    "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfac",
    "ledger": {
        "id": 2,
        "uuid": "ef34979b-cc6b-4181-a9d0-885c0bbd817b",
        "name": "My First Ledger",
        "description": "A collection of my accounts",
        "createdDate": "2024-06-12T10:20:29.424421Z",
        "lastUpdatedDate": "2024-06-12T10:20:29.424475Z"
    },
    "name": "My First Ledger Account",
    "description": "This is used to hold account transactions",
    "currency": "USD",
    "createdDate": "2024-06-12T10:20:29.653099Z",
    "lastUpdatedDate": "2024-06-12T10:20:29.653133Z"
}
```

**Create Ledger Transaction:** `http://<host>:<port>/api/ledger_transaction`

Request:
```json
{
    "uuid": "a1d968c1-86fc-4864-a146-f7f8e601fa3f",
    "description": "My First Transaction",
    "ledgerEntries": [
        {
            "ledgerAccount": {
                "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfab"
            },
            "amount": 100,
            "direction": "debit"
        },
        {
            "ledgerAccount": {
                "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfac"
            },
            "amount": 100,
            "direction": "credit"
        }
    ]
}
```

Response:
```json
{
    "status": "completed",
    "ledgerTransaction": {
        "id": 1,
        "uuid": "a1d968c1-86fc-4864-a146-f7f8e601fa3f",
        "description": "My First Transaction",
        "ledgerEntries": [
            {
                "id": 1,
                "ledgerAccount": {
                    "id": 1,
                    "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfab"
                },
                "amount": 100,
                "direction": "debit",
                "createdDate": "2024-06-13T23:07:34.571277612Z"
            },
            {
                "id": 2,
                "ledgerAccount": {
                    "id": 2,
                    "uuid": "a42f714c-ce90-4a9c-a06e-9a1b8842dfac"
                },
                "amount": 100,
                "direction": "credit",
                "createdDate": "2024-06-13T23:07:34.576605372Z"
            }
        ],
        "createdDate": "2024-06-13T23:07:34.615549765Z"
    }
}
```

**Get Ledger Balance:** `http://<host>:<port>/api/get_balance?uuid=a42f714c-ce90-4a9c-a06e-9a1b8842dfac&timestamp=2024-06-22T03:38:41.532%2B00:00`

Response:
```json
{
    "uuid": "316df247-cf38-4695-9f80-c63f6c72f678",
    "name": "My First Ledger",
    "description": "A collection of my accounts",
    "totalDebits": 25,
    "totalCredits": 0,
    "timestamp": "2024-06-22T03:38:41.532Z"
}
```

## Things still TODO
- Add more descriptions in swagger API documentation
- move all repository classes to common package?
- Unit test coverage is not exhaustive. I've only included a few major classes as an example.
- No user authentication/authorization for any requests. Could use JWT to avoid hit to DB and to improve scalability.
- Setup a Jenkins server to run tests/redeploy via Git hooks.
- Get Ledger should return list of Accounts
  - Get Accounts should return list of transactions
- Implement optimistic locking on ledger accounts table

## References

* https://www.moderntreasury.com/journal/designing-ledgers-with-optimistic-locking
* https://www.baeldung.com/ops/kafka-docker-setup
* https://www.baeldung.com/ops/kafka-new-topic-docker-compose
* https://www.baeldung.com/spring-kafka
* https://www.baeldung.com/spring-rest-openapi-documentation
* https://medium.com/@saygiligozde/using-docker-compose-with-spring-boot-and-postgresql-235031106f9f
* https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa