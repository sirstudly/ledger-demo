package demo.ledger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateLedgerStepDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger( CreateLedgerStepDefinitions.class );
    private final HttpClient httpClient;
    private final Gson gson;
    private final String ledgerUrl;
    private JsonObject request;
    private HttpResponse<String> response;

    public CreateLedgerStepDefinitions( HttpClient httpClient, Gson gson, @Value( value = "${demo.ledger.restserver.baseurl}" ) String baseUrl ) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.ledgerUrl = baseUrl + "/api/ledger";
    }

    @When( "I submit a POST request to create a new ledger" )
    public void createNewLedger() throws Exception {

        request = new JsonObject();
        request.addProperty( "uuid", UUID.randomUUID().toString() );
        request.addProperty( "name", "My first ledger" );
        request.addProperty( "description", "Ledger containing all my transactions" );
        LOGGER.info( "request={}", gson.toJson( request ) );

        // https://www.baeldung.com/java-httpclient-post
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerUrl ) )
                .header( "Content-Type", "application/json" )
                .POST( HttpRequest.BodyPublishers.ofString( gson.toJson( request ) ) )
                .build();

        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        LOGGER.info( "response={}", response.body() );
    }

    @Then( "the status code is {int}" )
    public void statusCodeIs( int statusCode ) {
        assertThat( response.statusCode() ).isEqualTo( statusCode );
    }

    @And( "the status is {string}" )
    public void responseStatusIs( String status ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        assertThat( resp.get( "status" ).getAsString() ).isEqualTo( status );
    }

    @And( "the {string} ledger field matches that in the request" )
    public void ledgerFieldMatchesRequest( String fieldName ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        JsonObject ledger = resp.get( "ledger" ).getAsJsonObject();
        assertThat( ledger.get( fieldName ).getAsString() ).isEqualTo( request.get( fieldName ).getAsString() );
    }

    @And( "the {string} ledger field is a non-zero integer" )
    public void ledgerFieldNonZeroInteger( String fieldName ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        JsonObject ledger = resp.get( "ledger" ).getAsJsonObject();
        assertThat( ledger.get( fieldName ).getAsBigInteger() ).isPositive();
    }

    @And( "I submit a GET request to retrieve the ledger by its UUID" )
    public void queryLedgerByUUID() throws Exception {
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerUrl + "/" + request.get( "uuid" ).getAsString() ) )
                .GET()
                .build();
        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
    }

    @And( "the {string} field matches that in the request" )
    public void fieldMatchesRequest( String fieldName ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        assertThat( resp.get( fieldName ).getAsString() ).isEqualTo( request.get( fieldName ).getAsString() );
    }

    @And( "the {string} field is a non-zero integer" )
    public void fieldNonZeroInteger( String fieldName ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        assertThat( resp.get( fieldName ).getAsBigInteger() ).isPositive();
    }
}
