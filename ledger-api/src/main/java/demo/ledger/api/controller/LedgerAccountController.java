package demo.ledger.api.controller;

import com.google.gson.Gson;
import demo.ledger.api.config.KafkaTopicConfig;
import demo.ledger.api.model.dto.CreateLedgerAccountRequest;
import demo.ledger.api.model.dto.RequestStatus;
import demo.ledger.api.model.dto.RestResponse;
import demo.ledger.api.model.dto.UuidLookup;
import demo.ledger.api.model.exception.NotFoundException;
import demo.ledger.api.service.LedgerService;
import demo.ledger.model.LedgerAccount;
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
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/api/ledger_account" )
public class LedgerAccountController extends BaseController {

    private Gson gson;
    private final LedgerService ledgerService;

    @Autowired
    public LedgerAccountController( KafkaTemplate<String, String> kafkaTemplate, Gson gson, LedgerService ledgerService ) {
        super( kafkaTemplate );
        this.gson = gson;
        this.ledgerService = ledgerService;
    }

    @Operation( summary = "Fetch a ledger account by its UUID" )
    @ApiResponses( value = {
            @ApiResponse( responseCode = "200", description = "Found the ledger account",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = CreateLedgerAccountRequest.class ) )} ),
            @ApiResponse( responseCode = "400", description = "Invalid uuid supplied",
                    content = @Content ),
            @ApiResponse( responseCode = "404", description = "Ledger account not found",
                    content = @Content )} )
    @GetMapping( "/{uuid}" )
    public LedgerAccount findByUuid(
            @Parameter( description = "UUID of ledger account to find" )
            @Valid UuidLookup lookup ) throws NotFoundException {

        return ledgerService.findLedgerAccountByUuid( lookup.getUuid() )
                .orElseThrow( () -> new NotFoundException( "No matching ledger account found." ) );
    }

    @Operation( summary = "Create a new ledger account" )
    @ApiResponses( value = {
            @ApiResponse( responseCode = "202", description = "Request submitted",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = RestResponse.class,
                                    description = "Contains the UUID of the ledger account to be created and the status" ) )} ),
            @ApiResponse( responseCode = "400", description = "Invalid request",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = RestResponse.class,
                                    description = "Contains the errors in the request and the status" ) )} ),
    } )
    @PostMapping
    @ResponseStatus( HttpStatus.ACCEPTED )
    public RestResponse createLedgerAccount(
            @Valid @RequestBody final CreateLedgerAccountRequest request ) {

        String payload = gson.toJson( new ApiOperation<CreateLedgerAccountRequest>()
                .withEventType( EventType.CREATE_LEDGER_ACCOUNT )
                .withData( request ) );

        // FIXME: "key" should be tied to the user so they write to the same partition (and are processed in the order it is received)
        // https://stackoverflow.com/a/58450517
        sendMessage( "USERID", payload );
        return new RestResponse( RequestStatus.pending );
    }

    @Override // FIXME: consolidate this class with LedgerController since we use the same event topic?
    public String getEventTopic() {
        return KafkaTopicConfig.LEDGER_EVENTS_TOPIC;
    }
}
