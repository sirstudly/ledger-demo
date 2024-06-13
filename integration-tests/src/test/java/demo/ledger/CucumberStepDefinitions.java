package demo.ledger;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.UUID;

public class CucumberStepDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger( CucumberStepDefinitions.class );
    private CucumberTestState state;

    public CucumberStepDefinitions( CucumberTestState state ) {
        this.state = state;
    }

    public CucumberTestState getState() {
        return state;
    }

    @Given( "I already have an existing ledger for {word}" )
    public void createNewLedgerAssertCompleted( String ledgerName ) throws Exception {
        getState()
                .createNewLedger( UUID.randomUUID().toString(), ledgerName + "'s first ledger" )
                .statusCodeIs( 202 )
                .responseStatusIs( "completed" )
                .saveLedgerUuidKeyedByName( ledgerName );
    }

    @When( "I submit a POST request to create a new ledger" )
    public void createNewLedger() throws Exception {
        getState().createNewLedger( UUID.randomUUID().toString(), "My first ledger" );
    }

    @When( "I submit a POST request to create a new ledger with the previous UUID" )
    public void createNewLedgerWithPreviousUUID() throws Exception {
        getState().createNewLedger( getState().getLedgerUuid(), "My first ledger" );
    }

    @When( "I submit a POST request to create a new ledger account for {word}" )
    public void createNewLedgerAccount( String accountName ) throws Exception {
        getState().createNewLedgerAccount( UUID.randomUUID().toString(), accountName + "'s account" );
    }

    @Given( "I already have an existing ledger account for {word}" )
    public void createNewLedgerAccountAssertCompleted( String accountName ) throws Exception {
        getState()
                .createNewLedgerAccount( UUID.randomUUID().toString(), accountName + "'s first account" )
                .statusCodeIs( 202 )
                .responseStatusIs( "completed" )
                .saveLedgerAccountUuidKeyedByName( accountName );
    }

    @When( "I submit a POST request to create a new ledger account with the previous UUID" )
    public void createNewLedgerAccountWithPreviousUUID() throws Exception {
        getState().createNewLedgerAccount( getState().getLedgerAccountUuid(), "Another ledger account" );
    }

    @Then( "the status code is {int}" )
    public void statusCodeIs( int statusCode ) {
        getState().statusCodeIs( statusCode );
    }

    @Then( "the status is {string}" )
    public void responseStatusIs( String status ) {
        getState().responseStatusIs( status );
    }

    @Then( "the {string} {word} field matches that in the request" )
    public void fieldMatchesRequest( String fieldName, String parentField ) {
        getState().fieldMatchesRequest( parentField, fieldName );
    }

    @Then( "the {string} {word} field is a non-zero integer" )
    public void fieldNonZeroInteger( String fieldName, String parentField ) {
        getState().fieldNonZeroInteger( parentField, fieldName );
    }

    @When( "I submit a GET request to retrieve the ledger by its UUID" )
    public void queryLedgerByUUID() throws Exception {
        getState().queryLedgerByUUID();
    }

    @When( "I submit a GET request to retrieve the ledger account by its UUID" )
    public void queryLedgerAccountByUUID() throws Exception {
        getState().queryLedgerAccountByUUID();
    }

    @Then( "the {string} field matches that in the request" )
    public void fieldMatchesRequest( String fieldName ) {
        getState().fieldMatchesRequest( fieldName );
    }

    @Then( "the {string} field is a non-zero integer" )
    public void fieldNonZeroInteger( String fieldName ) {
        getState().fieldNonZeroInteger( fieldName );
    }

    @Then( "the {string} field is {string}" )
    public void fieldMatchesValue( String fieldName, String value ) {
        getState().fieldMatches( fieldName, value );
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////            LEDGER TRANSACTION
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Given( "I have an existing ledger and account for {word}" )
    public void givenAnExistingLedgerAndAccountFor( String accountName ) throws Exception {
        createNewLedgerAssertCompleted( accountName );
        createNewLedgerAccountAssertCompleted( accountName );
    }

    @When( "I submit a POST transaction to debit {word} for ${int} and credit {word} for ${int}" )
    public void postNewLedgerTransaction( String fromAccountName, int debitAmount, String toAccountName, int creditAmount ) throws Exception {
        getState().createNewLedgerTransaction( UUID.randomUUID().toString(),
                "Debiting from " + fromAccountName + " the amount of $" + debitAmount
                        + " and transferring to " + toAccountName + " the amount of $" + creditAmount,
                new BigInteger( String.valueOf( debitAmount ) ), new BigInteger( String.valueOf( creditAmount ) ),
                fromAccountName, toAccountName
        );
    }

    @Then( "there exists a \\(transaction) ledger entry {word}ing {word} for ${int}" )
    public void transactionLedgerEntryExists( String direction, String accountName, int amount ) {
        getState().transactionLedgerEntryExists( direction, accountName, amount );
    }

    @Then( "there exists a ledger entry {word}ing {word} for ${int}" )
    public void ledgerEntryExists( String direction, String accountName, int amount ) {
        getState().ledgerEntryExists( direction, accountName, amount );
    }

    @When( "I submit a GET request to retrieve the ledger transaction by its UUID" )
    public void queryLedgerTransactionByUUID() throws Exception {
        getState().queryLedgerTransactionByUUID();
    }
}
