package demo.ledger.api.controller;

import com.google.gson.Gson;
import demo.ledger.api.config.KafkaTopicConfig;
import demo.ledger.api.model.dto.CreateLedgerRequest;
import demo.ledger.api.model.dto.CreateLedgerResponse;
import demo.ledger.api.model.dto.RestResponse;
import demo.ledger.api.model.dto.UuidLookup;
import demo.ledger.api.model.exception.NotFoundException;
import demo.ledger.api.service.LedgerService;
import demo.ledger.model.Ledger;
import demo.ledger.model.dto.ApiOperation;
import demo.ledger.model.dto.EventType;
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
@RequestMapping( "/api/ledger" )
public class LedgerController extends BaseController {

    private final Gson gson;
    private final LedgerService ledgerService;

    @Value( value = "${ledger.api.service.timeout.ms}" )
    private long API_SERVICE_TIMEOUT_MS;

    @Autowired
    public LedgerController( KafkaTemplate<String, String> kafkaTemplate, Gson gson, LedgerService ledgerService ) {
        super( kafkaTemplate );
        this.gson = gson;
        this.ledgerService = ledgerService;
    }

    @Operation( summary = "Fetch a ledger by its UUID" )
    @ApiResponses( value = {
            @ApiResponse( responseCode = "200", description = "Found the ledger",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = CreateLedgerRequest.class ) )} ),
            @ApiResponse( responseCode = "400", description = "Invalid id supplied",
                    content = @Content ),
            @ApiResponse( responseCode = "404", description = "Ledger not found",
                    content = @Content )} )
    @GetMapping( "/{uuid}" )
    public Ledger findByUuid(
            @Parameter( description = "UUID of ledger to find" )
            @Valid UuidLookup lookup ) throws NotFoundException {

        return ledgerService.findLedgerByUuid( lookup.getUuid() )
                .orElseThrow( () -> new NotFoundException( "No matching ledger found." ) );
    }

    @Operation( summary = "Create a new ledger" )
    @ApiResponses( value = {
            @ApiResponse( responseCode = "202", description = "Request submitted",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = CreateLedgerResponse.class,
                                    description = "Contains the UUID of the ledger to be created and the status" ) )} ),
            @ApiResponse( responseCode = "400", description = "Invalid request",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = RestResponse.class,
                                    description = "Contains the errors in the request and the status" ) )} ),
    } )
    @PostMapping
    @ResponseStatus( HttpStatus.ACCEPTED )
    public CreateLedgerResponse createLedger(
            @Valid @RequestBody final CreateLedgerRequest request ) {

        String payload = gson.toJson( new ApiOperation<CreateLedgerRequest>()
                .withEventType( EventType.CREATE_LEDGER )
                .withData( request ) );

        // TODO: "key" should be tied to the user so they write to the same partition (and are processed in the order it is received)
        // https://stackoverflow.com/a/58450517
        sendMessage( "USERID", payload );
        return ledgerService.waitForLedgerCreation( request.getUuid(), API_SERVICE_TIMEOUT_MS );
    }

    @Override
    public String getEventTopic() {
        return KafkaTopicConfig.LEDGER_EVENTS_TOPIC;
    }
}