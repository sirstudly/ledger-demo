package demo.ledger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class CucumberTestState {
    private static final Logger LOGGER = LoggerFactory.getLogger( CucumberTestState.class );
    private final HttpClient httpClient;
    private final Gson gson;
    private final String ledgerUrl;
    private final String ledgerAccountUrl;
    private final String ledgerTransactionUrl;
    private final String getBalanceUrl;

    // fixed seed so that our tests are consistent and replayable
    private final long RANDOM_SEED = 740248611806L;
    private final Random random = new Random( RANDOM_SEED );

    // for simplicity, we only keep track of the last request/response
    private JsonObject request;
    private HttpResponse<String> response;
    private String ledgerUuid;
    private String ledgerAccountUuid;
    private String ledgerTransactionUuid;

    // the following are maps keyed by account (name)
    private final Map<String, String> ledgerUuidMap = new HashMap<>();
    private final Map<String, String> ledgerAccountUuidMap = new HashMap<>();
    private final Map<String, HttpResponse<String>> getBalanceResponseMap = new HashMap<>();
    private final Map<String, BigInteger> debitTotalMap = new HashMap<>();
    private final Map<String, BigInteger> creditTotalMap = new HashMap<>();

    public CucumberTestState( HttpClient httpClient, Gson gson, String baseUrl ) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.ledgerUrl = baseUrl + "/api/ledger";
        this.ledgerAccountUrl = baseUrl + "/api/ledger_account";
        this.ledgerTransactionUrl = baseUrl + "/api/ledger_transaction";
        this.getBalanceUrl = baseUrl + "/api/get_balance";
    }

    public CucumberTestState createNewLedger( String uuid, String ledgerName ) throws Exception {
        request = new JsonObject();
        request.addProperty( "uuid", uuid );
        request.addProperty( "name", ledgerName );
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

    public CucumberTestState createNewLedgerAccount( String uuid, String accountName ) throws Exception {

        assertThat( ledgerUuid ).isNotNull();
        request = new JsonObject();
        request.addProperty( "uuid", uuid );
        request.addProperty( "name", accountName );
        request.addProperty( "description", "Ledger account containing all my transactions" );
        request.addProperty( "currency", "USD" );
        request.add( "ledger", uuidLookup( ledgerUuid ) );
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

    /**
     * Creates a new ledger transaction and submits it to the REST server.
     *
     * @param uuid              unique ID for this transaction
     * @param description       description of this transaction
     * @param debitAmount       amount being debited in base units
     * @param creditAmount      amount being credited in base units
     * @param fromLedgerAccount the account we are debiting from
     * @param toLedgerAccount   the account we are crediting to
     * @return this object
     * @throws Exception
     */
    public CucumberTestState createNewLedgerTransaction( String uuid, String description, BigInteger debitAmount, BigInteger creditAmount,
                                                         String fromLedgerAccount, String toLedgerAccount ) throws Exception {

        String fromLedgerUuid = getLedgerUuid( fromLedgerAccount );
        String toLedgerUuid = getLedgerUuid( toLedgerAccount );
        assertThat( fromLedgerUuid ).isNotNull();
        assertThat( toLedgerUuid ).isNotNull();

        String fromLedgerAccountUuid = getLedgerAccountUuid( fromLedgerAccount );
        String toLedgerAccountUuid = getLedgerAccountUuid( toLedgerAccount );
        assertThat( fromLedgerAccountUuid ).isNotNull();
        assertThat( toLedgerAccountUuid ).isNotNull();

        // build up our transaction object
        request = new JsonObject();
        request.addProperty( "uuid", uuid );
        request.addProperty( "description", description );
        JsonArray ledgerEntries = new JsonArray();
        ledgerEntries.add( ledgerEntry( fromLedgerAccountUuid, "debit", debitAmount ) );
        ledgerEntries.add( ledgerEntry( toLedgerAccountUuid, "credit", creditAmount ) );
        request.add( "ledgerEntries", ledgerEntries );
        LOGGER.info( "request={}", gson.toJson( request ) );

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerTransactionUrl ) )
                .header( "Content-Type", "application/json" )
                .POST( HttpRequest.BodyPublishers.ofString( gson.toJson( request ) ) )
                .build();

        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        LOGGER.info( "response={}", response.body() );

        // save this for later...
        ledgerTransactionUuid = request.get( "uuid" ).getAsString();

        return this;
    }

    /**
     * Checks that the last response contains a ledger transaction -&gt; ledger entry with the following details.
     *
     * @param direction   either credit or debit
     * @param accountName the name of the account to lookup the UUID for
     * @param amount      the amount to debit/credit
     * @return this object
     */
    public CucumberTestState transactionLedgerEntryExists( String direction, String accountName, int amount ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        return ledgerEntryExists( resp.get( "ledgerTransaction" ).getAsJsonObject(), direction, accountName, amount );
    }

    /**
     * Checks that the last response contains a ledger entry with the following details.
     *
     * @param direction   either credit or debit
     * @param accountName the name of the account to lookup the UUID for
     * @param amount      the amount to debit/credit
     * @return this object
     */
    public CucumberTestState ledgerEntryExists( String direction, String accountName, int amount ) {
        JsonObject resp = gson.fromJson( response.body(), JsonObject.class );
        return ledgerEntryExists( resp.getAsJsonObject(), direction, accountName, amount );
    }

    private CucumberTestState ledgerEntryExists( JsonObject resp, String direction, String accountName, int amount ) {
        String accountUuid = getLedgerAccountUuid( accountName );
        JsonElement matchedEntry = resp.get( "ledgerEntries" ).getAsJsonArray().asList().stream().filter(
                        e -> accountUuid.equals( e.getAsJsonObject()
                                .get( "ledgerAccount" ).getAsJsonObject()
                                .get( "uuid" ).getAsString() ) ).findAny()
                .orElseThrow( () -> new AssertionError( "Missing ledger entry for account " + accountName ) );
        assertThat( matchedEntry.getAsJsonObject().get( "amount" ).getAsInt() ).isEqualTo( amount );
        assertThat( matchedEntry.getAsJsonObject().get( "direction" ).getAsString() ).isEqualTo( direction );
        assertThat( matchedEntry.getAsJsonObject().get( "id" ).getAsBigInteger() ).isPositive();
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
        LOGGER.info( "response={}", response.body() );
        return this;
    }

    public CucumberTestState queryLedgerAccountByUUID() throws Exception {
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerAccountUrl + "/" + request.get( "uuid" ).getAsString() ) )
                .GET()
                .build();
        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        LOGGER.info( "response={}", response.body() );
        return this;
    }

    public CucumberTestState queryLedgerTransactionByUUID() throws Exception {
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( ledgerTransactionUrl + "/" + request.get( "uuid" ).getAsString() ) )
                .GET()
                .build();
        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        LOGGER.info( "response={}", response.body() );
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

    public CucumberTestState saveLedgerUuidKeyedByName( String name ) {
        ledgerUuidMap.put( name, ledgerUuid );
        return this;
    }

    public CucumberTestState saveLedgerAccountUuidKeyedByName( String name ) {
        ledgerAccountUuidMap.put( name, ledgerAccountUuid );
        return this;
    }

    /**
     * Create a random number of transactions between account1 and account2.
     *
     * @param numTransactions number of transactions to submit
     * @param account1        one of the account names
     * @param account2        one of the other account names
     * @return this object
     * @throws Exception on submission failure
     */
    public CucumberTestState submitRandomTransactions( int numTransactions, String account1, String account2 ) throws Exception {
        for ( int i = 0; i < numTransactions; i++ ) {
            final boolean chooseWisely = random.nextBoolean();
            final BigInteger transferAmount = new BigInteger( String.valueOf( random.nextInt( 10000 ) ) );

            // each transaction is a double-entry (one debit and one credit)
            LOGGER.info( "Submitting transfer transaction for " + transferAmount
                    + " from " + ( chooseWisely ? account1 : account2 )
                    + " to " + ( chooseWisely ? account2 : account1 ) );
            createNewLedgerTransaction( UUID.randomUUID().toString(),
                    "Random transaction #" + i + " between " + account1 + " and " + account2,
                    transferAmount,
                    transferAmount,
                    chooseWisely ? account1 : account2,
                    chooseWisely ? account2 : account1 );
            statusCodeIs( 202 );
            responseStatusIs( "completed" );

            // keep track of the debit/credit totals so we can compare later
            debitTotalMap.merge( chooseWisely ? account1 : account2, transferAmount, BigInteger::add );
            creditTotalMap.merge( chooseWisely ? account2 : account1, transferAmount, BigInteger::add );
        }

        debitTotalMap.forEach( ( key, value ) -> LOGGER.info( "DEBIT total: " + key + " -> " + value ) );
        creditTotalMap.forEach( ( key, value ) -> LOGGER.info( "CREDIT total: " + key + " -> " + value ) );
        return this;
    }

    public CucumberTestState submitBalanceRequest( String accountName ) throws Exception {

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri( URI.create( getBalanceUrl + "?uuid=" + getLedgerAccountUuid( accountName ) ) )
                .GET()
                .build();

        response = httpClient.send( httpReq, HttpResponse.BodyHandlers.ofString() );
        LOGGER.info( "response={}", response.body() );

        // keep track of the balances so we can query them later
        getBalanceResponseMap.put( accountName, response );
        return this;
    }

    /**
     * Verifies the debit/credit totals from previous transactions add up to the balance requests.
     *
     * @return this object
     */
    public CucumberTestState validateDebitCreditTotals() {

        // go through the get balance responses and check that the debit/credit totals are what we expect
        getBalanceResponseMap.forEach( ( account, resp ) -> {
            JsonObject obj = gson.fromJson( resp.body(), JsonObject.class );
            BigInteger totalDebits = obj.get( "totalDebits" ).getAsBigInteger();
            BigInteger totalCredits = obj.get( "totalCredits" ).getAsBigInteger();
            LOGGER.info( "GetBalance for " + account + " says TOTAL DEBITS is " + totalDebits );
            LOGGER.info( "GetBalance for " + account + " says TOTAL CREDITS is " + totalCredits );

            // compare with what we submited in our transaction requests
            assertThat( debitTotalMap.get( account ) ).as( "Checking debit total for %s", account ).isEqualTo( totalDebits );
            assertThat( creditTotalMap.get( account ) ).as( "Checking credit total for %s", account ).isEqualTo( totalCredits );
        } );
        return this;
    }

    public CucumberTestState validateDebitAmountAndCreditTotalsAreTheSame() {
        final AtomicReference<BigInteger> totalDebits = new AtomicReference<>();
        final AtomicReference<BigInteger> totalCredits = new AtomicReference<>();
        totalDebits.set( BigInteger.ZERO );
        totalCredits.set( BigInteger.ZERO );

        // total up all the debits/credits from the balance requests
        getBalanceResponseMap.forEach( ( account, resp ) -> {
            JsonObject obj = gson.fromJson( resp.body(), JsonObject.class );
            totalDebits.set( totalDebits.get().add( obj.get( "totalDebits" ).getAsBigInteger() ) );
            totalCredits.set( totalCredits.get().add( obj.get( "totalCredits" ).getAsBigInteger() ) );
        } );

        // total debits == total credits
        assertThat( totalDebits.get() ).isEqualTo( totalCredits.get() );

        // total up all the debits/credits from the original transactions
        BigInteger debitTotal = debitTotalMap.values().stream().reduce( BigInteger.ZERO, BigInteger::add );
        BigInteger creditTotal = creditTotalMap.values().stream().reduce( BigInteger.ZERO, BigInteger::add );

        // total debits == total credits
        assertThat( debitTotal ).isEqualTo( creditTotal );
        return this;
    }

    public String getLedgerUuid() {
        return this.ledgerUuid;
    }

    public String getLedgerAccountUuid() {
        return this.ledgerAccountUuid;
    }

    public String getLedgerUuid( String key ) {
        return ledgerUuidMap.get( key );
    }

    public String getLedgerAccountUuid( String key ) {
        return ledgerAccountUuidMap.get( key );
    }

    private JsonObject uuidLookup( String uuid ) {
        JsonObject uuidLookup = new JsonObject();
        uuidLookup.addProperty( "uuid", uuid );
        return uuidLookup;
    }

    private JsonObject ledgerEntry( String ledgerAccountUuid, String direction, BigInteger amount ) {
        JsonObject entry = new JsonObject();
        entry.add( "ledgerAccount", uuidLookup( ledgerAccountUuid ) );
        entry.addProperty( "direction", direction );
        entry.addProperty( "amount", amount );
        return entry;
    }
}
