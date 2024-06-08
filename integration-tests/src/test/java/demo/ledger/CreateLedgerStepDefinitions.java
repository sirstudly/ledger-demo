package demo.ledger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.UUID;

public class CreateLedgerStepDefinitions {

    private HttpClient client = HttpClient.newHttpClient();

    @When("I submit a POST request to create a new ledger")
    public void createNewLedger() {

        JsonObject req = new JsonObject();
        req.addProperty("uuid", UUID.randomUUID().toString());
        req.addProperty( "name", "My first ledger" );
        req.addProperty("description", "Ledger containing all my transactions");

//        HttpRequest request = HttpRequest.newBuilder()
//                .uri( URI.create("http://localhost:6868/api/ledger"))
//                .POST(HttpRequest.BodyPublishers.ofString(  ))
//                .build();
    }

    @Then("I can submit a GET request to retrieve the ledger by it's UUID")
    public void queryLedgerByUUID() {
    }

}
