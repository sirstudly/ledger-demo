Feature: Creating two different ledger accounts and record a transfer between the two accounts
  As an API user
  I want to record a transaction moving from one account to another
  So that I can keep track of all my users account transactions

  Scenario: Create two ledger accounts and record a transaction by debiting one account and crediting another
    Given I have an existing ledger and account for Audrey
    And I have an existing ledger and account for Bronson
    When I submit a POST transaction to debit Audrey for $25 and credit Bronson for $25
    Then the status code is 202
    And the status is 'completed'
    And the 'uuid' ledgerTransaction field matches that in the request
    And the 'description' ledgerTransaction field matches that in the request
    And there exists a (transaction) ledger entry debiting Audrey for $25
    And there exists a (transaction) ledger entry crediting Bronson for $25
    # id represents the primary key in the ledger transaction table
    And the 'id' ledgerTransaction field is a non-zero integer

    When I submit a GET request to retrieve the ledger transaction by its UUID
    And the status code is 200
    And the 'uuid' field matches that in the request
    And the 'description' field matches that in the request
    And the 'id' field is a non-zero integer
    And there exists a ledger entry debiting Audrey for $25
    And there exists a ledger entry crediting Bronson for $25
