package demo.ledger.api.controller;

import com.google.gson.Gson;
import demo.ledger.api.config.KafkaTopicConfig;
import demo.ledger.api.model.dto.CreateLedgerAccountRequest;
import demo.ledger.api.model.dto.CreateLedgerAccountResponse;
import demo.ledger.api.model.dto.CreateLedgerTransactionRequest;
import demo.ledger.api.model.dto.RestResponse;
import demo.ledger.api.model.dto.UuidLookup;
import demo.ledger.api.service.LedgerService;
import demo.ledger.model.LedgerAccount;
import demo.ledger.model.LedgerTransaction;
import demo.ledger.model.dto.ApiOperation;
import demo.ledger.model.dto.EventType;
import demo.ledger.model.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/api/ledger_transaction" )
public class LedgerTransactionController extends BaseController {

    private Gson gson;
    private final LedgerService ledgerService;

    @Value( value = "${ledger.api.service.timeout.ms}" )
    private long API_SERVICE_TIMEOUT_MS;

    @Autowired
    public LedgerTransactionController( KafkaTemplate<String, String> kafkaTemplate, Gson gson, LedgerService ledgerService ) {
        super( kafkaTemplate );
        this.gson = gson;
        this.ledgerService = ledgerService;
    }

    @Operation( summary = "Fetch a ledger transaction by its UUID" )
    @ApiResponses( value = {
            @ApiResponse( responseCode = "200", description = "Found the ledger transaction",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = CreateLedgerAccountResponse.class ) )} ),
            @ApiResponse( responseCode = "400", description = "Invalid uuid supplied",
                    content = @Content ),
            @ApiResponse( responseCode = "404", description = "Ledger transaction not found",
                    content = @Content )} )
    @GetMapping( "/{uuid}" )
    public LedgerTransaction findByUuid(
            @Parameter( description = "UUID of ledger transaction to find" )
            @Valid UuidLookup lookup ) throws NotFoundException {

        return ledgerService.findLedgerTransactionByUuid( lookup.getUuid() )
                .orElseThrow( () -> new NotFoundException( "No matching ledger transaction found." ) );
    }

    @Operation( summary = "Create a new ledger transaction" )
    @ApiResponses( value = {
            @ApiResponse( responseCode = "202", description = "Request submitted",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = RestResponse.class,
                                    description = "Contains the UUID of the ledger transaction to be created and the status" ) )} ),
            @ApiResponse( responseCode = "400", description = "Invalid request",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = RestResponse.class,
                                    description = "Contains the errors in the request and the status" ) )} ),
    } )
    @PostMapping
    @ResponseStatus( HttpStatus.ACCEPTED )
    public RestResponse createLedgerTransaction(
            @Valid @RequestBody final CreateLedgerTransactionRequest request ) {

        String payload = gson.toJson( new ApiOperation<CreateLedgerTransactionRequest>()
                .withEventType( EventType.CREATE_LEDGER_TRANSACTION )
                .withData( request ) );

        // FIXME: "key" should be tied to the user so they write to the same partition (and are processed in the order it is received)
        // https://stackoverflow.com/a/58450517
        sendMessage( "USERID", payload );
        return ledgerService.waitForLedgerTransactionCreation( request.getUuid(), API_SERVICE_TIMEOUT_MS );
    }

    @Override
    public String getEventTopic() {
        return KafkaTopicConfig.LEDGER_EVENTS_TOPIC;
    }
}
