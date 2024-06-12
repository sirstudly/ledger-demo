package demo.ledger;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Given( "I already have an existing ledger" )
    public void createNewLedgerAssertCompleted() throws Exception {
        getState()
                .createNewLedger( UUID.randomUUID().toString() )
                .statusCodeIs( 202 )
                .responseStatusIs( "completed" );
    }

    @When( "I submit a POST request to create a new ledger" )
    public void createNewLedger() throws Exception {
        getState().createNewLedger( UUID.randomUUID().toString() );
    }

    @When( "I submit a POST request to create a new ledger with the previous UUID" )
    public void createNewLedgerWithPreviousUUID() throws Exception {
        getState().createNewLedger( getState().getLedgerUuid() );
    }

    @When( "I submit a POST request to create a new ledger account" )
    public void createNewLedgerAccount() throws Exception {
        getState().createNewLedgerAccount( UUID.randomUUID().toString() );
    }

    @Given( "I already have an existing ledger account" )
    public void createNewLedgerAccountAssertCompleted() throws Exception {
        getState()
                .createNewLedgerAccount( UUID.randomUUID().toString() )
                .statusCodeIs( 202 )
                .responseStatusIs( "completed" );
    }

    @When( "I submit a POST request to create a new ledger account with the previous UUID" )
    public void createNewLedgerAccountWithPreviousUUID() throws Exception {
        getState().createNewLedgerAccount( getState().getLedgerAccountUuid() );
    }

    @Then( "the status code is {int}" )
    public void statusCodeIs( int statusCode ) {
        getState().statusCodeIs( statusCode );
    }

    @And( "the status is {string}" )
    public void responseStatusIs( String status ) {
        getState().responseStatusIs( status );
    }

    @And( "the {string} {word} field matches that in the request" )
    public void fieldMatchesRequest( String fieldName, String parentField ) {
        getState().fieldMatchesRequest( parentField, fieldName );
    }

    @And( "the {string} {word} field is a non-zero integer" )
    public void fieldNonZeroInteger( String fieldName, String parentField ) {
        getState().fieldNonZeroInteger( parentField, fieldName );
    }

    @And( "I submit a GET request to retrieve the ledger by its UUID" )
    public void queryLedgerByUUID() throws Exception {
        getState().queryLedgerByUUID();
    }

    @And( "I submit a GET request to retrieve the ledger account by its UUID" )
    public void queryLedgerAccountByUUID() throws Exception {
        getState().queryLedgerAccountByUUID();
    }

    @And( "the {string} field matches that in the request" )
    public void fieldMatchesRequest( String fieldName ) {
        getState().fieldMatchesRequest( fieldName );
    }

    @And( "the {string} field is a non-zero integer" )
    public void fieldNonZeroInteger( String fieldName ) {
        getState().fieldNonZeroInteger( fieldName );
    }

    @And( "the {string} field is {string}" )
    public void fieldMatchesValue( String fieldName, String value ) {
        getState().fieldMatches( fieldName, value );
    }
}
