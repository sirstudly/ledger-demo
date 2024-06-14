package demo.ledger.api.controller;

import demo.ledger.api.config.KafkaTopicConfig;
import demo.ledger.api.model.dto.GetBalanceResponse;
import demo.ledger.api.service.LedgerService;
import demo.ledger.model.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping( "/api/get_balance" )
public class GetBalanceController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger( GetBalanceController.class );
    private final LedgerService ledgerService;

    @Autowired
    public GetBalanceController( KafkaTemplate<String, String> kafkaTemplate, LedgerService ledgerService ) {
        super( kafkaTemplate );
        this.ledgerService = ledgerService;
    }

    @Operation( summary = "Fetch a ledger account balance by its UUID" )
    @ApiResponses( value = {
            @ApiResponse( responseCode = "200", description = "Found the ledger",
                    content = {@Content( mediaType = "application/json",
                            schema = @Schema( implementation = GetBalanceResponse.class ) )} ),
            @ApiResponse( responseCode = "400", description = "Invalid id supplied",
                    content = @Content ),
            @ApiResponse( responseCode = "404", description = "Ledger account not found",
                    content = @Content )} )
    @GetMapping
    public GetBalanceResponse findBalanceByUuid(

            @Pattern( regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
                    message = "Invalid UUID (only lowercase characters allowed)" )
            @Parameter( description = "UUID of ledger account to query" )
            @RequestParam( "uuid" ) String uuid,

            @Pattern( regexp = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T([0-9]{2}:){2}[0-9]{2}(\\.[0-9]{3})?[+|-][0-9]{2}:[0-9]{2}$",
                    message = "Date/time must be specified in the following format YYYY-MM-DDThh:mm:ss+HH:MM (eg. 2024-04-11T10:24:35+02:00)" )
            @Parameter( description = "UUID of ledger account to query" )
            @RequestParam( value = "timestamp", required = false ) String timestamp ) throws NotFoundException {

        LOGGER.info( "getBalance: UUID={}, timestamp={}", uuid, timestamp );
        return ledgerService.fetchLedgerAccountBalance( uuid, timestamp == null ? null : OffsetDateTime.parse( timestamp ) );
    }

    @Override
    public String getEventTopic() {
        return KafkaTopicConfig.LEDGER_EVENTS_TOPIC;
    }
}