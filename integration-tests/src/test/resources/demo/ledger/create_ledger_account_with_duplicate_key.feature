Feature: Creating a new ledger account with duplicate key fails with an appropriate message
  As an API user
  When I submit a create ledger account request with an exising UUID
  I should get an appropriate error message
  So that I can correct my request

  Scenario: Attempt to create a new ledger account with an existing account with the same UUID
    Given I already have an existing ledger
    And I already have an existing ledger account
    When I submit a POST request to create a new ledger account with the previous UUID
    Then the status code is 202
    And the status is 'failed'
    And the 'error' field is 'Ledger account already exists with this UUID'
    And the 'uuid' ledgerAccount field matches that in the request
