Feature: Creating a new ledger with duplicate key fails with an appropriate message
  As an API user
  When I submit a create ledger request with an exising UUID
  I should get an appropriate error message
  So that I can correct my request

  Scenario: Attempt to create a new ledger with an existing ledger with the same UUID
    Given I already have an existing ledger
    When I submit a POST request to create a new ledger with the previous UUID
    Then the status code is 202
    And the status is 'failed'
    And the 'error' field is 'Ledger already exists with this UUID'
    And the 'uuid' ledger field matches that in the request
