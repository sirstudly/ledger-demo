Feature: Creating a new ledger

  Scenario: Create a new ledger and check it can be queried
    When I submit a POST request to create a new ledger
    Then I can submit a GET request to retrieve the ledger by it's UUID
