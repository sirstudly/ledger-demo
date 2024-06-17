Feature: Submitting balance requests returns the correct sum of credits and debits
  As an API user
  I want to query the real-time account balances for a particular account
  So that I can keep track how much is going into/out of the account

  Scenario: Create multiple ledger accounts with a number of credit/debit transactions and query the account balance
  for a particular account and different points in time. The balance totals should match up with the transaction amounts.
    Given I have an existing ledger and account for Abbey
    And I have an existing ledger and account for Ben
    And I have an existing ledger and account for Charlie
    And I submit 20 random transactions between Abbey and Ben
    And I submit 20 random transactions between Ben and Charlie
    And I submit 20 random transactions between Abbey and Charlie
    When I submit a balance request for Abbey's account
    Then the status code is 200
    # each account takes part in 20 x 2 transactions (starting with a lock version of 1) = 41
    And the 'lockVersion' field is 41
    When I submit a balance request for Ben's account
    Then the status code is 200
    And the 'lockVersion' field is 41
    When I submit a balance request for Charlie's account
    Then the status code is 200
    And the 'lockVersion' field is 41
    And the debit and credit totals for all the balance requests sum to the same amount as all the random transactions
    And the total amount of all credits equals the total amount of all debits
