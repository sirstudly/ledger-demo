package demo.ledger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.OffsetDateTime;

@Entity
@Table( name = "ledger_entry" )
@JsonInclude( JsonInclude.Include.NON_NULL )
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue
    private Long id;

    // unidirectional: case 5
    // https://medium.com/@rajibrath20/the-best-way-to-map-a-onetomany-relationship-with-jpa-and-hibernate-dbbf6dba00d3
    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "ledger_account_id", nullable = false, referencedColumnName = "id" )
    private LedgerAccount ledgerAccount;

    @Column( name = "amount", nullable = false )
    private BigInteger amount;

    @Column( name = "direction", nullable = false )
    @Enumerated( EnumType.STRING )
    private LedgerTransactionDirection direction;

    @Column( name = "created_date", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE" )
    private OffsetDateTime createdDate;
}
