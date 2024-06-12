Feature: Creating a new ledger account
  As an API user
  I want to create a new ledger account
  So that I can keep track of my account transactions

  Scenario: Create a new ledger account and check it can be queried
    Given I already have an existing ledger
    When I submit a POST request to create a new ledger account
    Then the status code is 202
    And the status is 'completed'
    And the 'uuid' ledgerAccount field matches that in the request
    And the 'name' ledgerAccount field matches that in the request
    And the 'description' ledgerAccount field matches that in the request
    And the 'currency' ledgerAccount field matches that in the request
    # id represents the primary key in the ledger account table
    And the 'id' ledgerAccount field is a non-zero integer

    When I submit a GET request to retrieve the ledger account by its UUID
    And the status code is 200
    And the 'uuid' field matches that in the request
    And the 'name' field matches that in the request
    And the 'description' field matches that in the request
    And the 'currency' field matches that in the request
    And the 'id' field is a non-zero integer
