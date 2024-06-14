package demo.ledger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import demo.ledger.model.LedgerAccount;
import demo.ledger.model.LedgerTransactionDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table( name = "ledger_transaction" )
@JsonInclude( JsonInclude.Include.NON_NULL )
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerTransaction {

    @Id
    @GeneratedValue
    private Long id;

    @Column( name = "uuid", nullable = false, unique = true )
    private String uuid;

    @Column( name = "description", nullable = false )
    private String description;

    // unidirectional: case 4
    // https://medium.com/@rajibrath20/the-best-way-to-map-a-onetomany-relationship-with-jpa-and-hibernate-dbbf6dba00d3
    @OneToMany( targetEntity = LedgerEntry.class, fetch = FetchType.LAZY )
    @JoinColumn( name = "ledger_transaction_id", referencedColumnName = "id" )
    private List<LedgerEntry> ledgerEntries;

    @Column( name = "created_date", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE" )
    private OffsetDateTime createdDate;
}
