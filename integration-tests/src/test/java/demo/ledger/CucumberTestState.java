package demo.ledger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class CucumberTestState {
    private static final Logger LOGGER = LoggerFactory.getLogger( CucumberTestState.class );
    private final HttpClient httpClient;
    private final Gson gson;
    private final String ledgerUrl;
    private final String ledgerAccountUrl;

    // for simplicity, we only keep track of the last request/response
    private JsonObject request;
    private HttpResponse<String> response;
    private String ledgerUuid;
    private String ledgerAccountUuid;

    public CucumberTestState( HttpClient httpClient, Gson gson, String baseUrl ) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.ledgerUrl = baseUrl + "/api/ledger";
        this.ledgerAccountUrl = baseUrl + "/api/ledger_account";
    }

    public CucumberTestState createNewLedger( String uuid ) throws Exception {
        request = new JsonObject();
        request.addProperty( "uuid", uuid );
        request.addProperty( "name", "My first ledger" );
        request.addProperty( "description", "Ledger containing all my accounts" );
        LOGGER.info( "request={}", gson.toJson( request ) );

        // https://www.baeldung.com/java-httpclient-post
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerUrl ) )
                .header( "Content-Type", "application/json" )
                .POST( HttpRequest.BodyPublishers.ofString( gson.toJson( request ) ) )
                .build();

        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        LOGGER.info( "response={}", response.body() );

        // save this for later...
        ledgerUuid = request.get( "uuid" ).getAsString();

        return this;
    }

    public CucumberTestState createNewLedgerAccount( String uuid ) throws Exception {

        assertThat( ledgerUuid ).isNotNull();
        request = new JsonObject();
        request.addProperty( "uuid", uuid );
        request.addProperty( "name", "My first ledger account" );
        request.addProperty( "description", "Ledger account containing all my transactions" );
        request.addProperty( "currency", "USD" );
        JsonObject ledgerLookup = new JsonObject();
        ledgerLookup.addProperty( "uuid", ledgerUuid );
        request.add( "ledger", ledgerLookup );
        LOGGER.info( "request={}", gson.toJson( request ) );

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerAccountUrl ) )
                .header( "Content-Type", "application/json" )
                .POST( HttpRequest.BodyPublishers.ofString( gson.toJson( request ) ) )
                .build();

        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        LOGGER.info( "response={}", response.body() );

        // save this for later...
        ledgerAccountUuid = request.get( "uuid" ).getAsString();

        return this;
    }

    public CucumberTestState statusCodeIs( int statusCode ) {
        assertThat( response.statusCode() ).isEqualTo( statusCode );
        return this;
    }

    public CucumberTestState responseStatusIs( String status ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        assertThat( resp.get( "status" ).getAsString() ).isEqualTo( status );
        return this;
    }

    public CucumberTestState fieldMatchesRequest( String parentField, String fieldName ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        JsonObject ledger = resp.get( parentField ).getAsJsonObject();
        assertThat( ledger.get( fieldName ).getAsString() ).isEqualTo( request.get( fieldName ).getAsString() );
        return this;
    }

    public CucumberTestState fieldNonZeroInteger( String parentField, String fieldName ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        JsonObject ledger = resp.get( parentField ).getAsJsonObject();
        assertThat( ledger.get( fieldName ).getAsBigInteger() ).isPositive();
        return this;
    }

    public CucumberTestState queryLedgerByUUID() throws Exception {
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerUrl + "/" + request.get( "uuid" ).getAsString() ) )
                .GET()
                .build();
        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        return this;
    }

    public CucumberTestState queryLedgerAccountByUUID() throws Exception {
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerAccountUrl + "/" + request.get( "uuid" ).getAsString() ) )
                .GET()
                .build();
        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        return this;
    }

    public CucumberTestState fieldMatchesRequest( String fieldName ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        assertThat( resp.get( fieldName ).getAsString() ).isEqualTo( request.get( fieldName ).getAsString() );
        return this;
    }

    public CucumberTestState fieldNonZeroInteger( String fieldName ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        assertThat( resp.get( fieldName ).getAsBigInteger() ).isPositive();
        return this;
    }

    public CucumberTestState fieldMatches( String fieldName, String value ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        assertThat( resp.get( fieldName ).getAsString() ).isEqualTo( value );
        return this;
    }

    public String getLedgerUuid() {
        return this.ledgerUuid;
    }

    public String getLedgerAccountUuid() {
        return this.ledgerAccountUuid;
    }
}
