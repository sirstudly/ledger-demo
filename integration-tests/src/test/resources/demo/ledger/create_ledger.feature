Feature: Creating a new ledger

  Scenario: Create a new ledger and check it can be queried
    When I submit a POST request to create a new ledger
    Then the status code is 202
    And the status is 'completed'
    And the 'uuid' ledger field matches that in the request
    And the 'name' ledger field matches that in the request
    And the 'description' ledger field matches that in the request
    # id represents the primary key in the ledger table
    And the 'id' ledger field is a non-zero integer

    When I submit a GET request to retrieve the ledger by its UUID
    And the status code is 200
    And the 'uuid' field matches that in the request
    And the 'name' field matches that in the request
    And the 'description' field matches that in the request
    And the 'id' field is a non-zero integer
