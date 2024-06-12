package demo.ledger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.OffsetDateTime;

@Entity
@Table( name = "ledger_account" )
@JsonInclude( JsonInclude.Include.NON_NULL )
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerAccount {

    @Id
    @GeneratedValue
    private Long id;

    @Column( name = "uuid", nullable = false, unique = true )
    private String uuid;

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "ledger_id", nullable = false, referencedColumnName = "id" )
    private Ledger ledger;

    @Column( name = "name", nullable = false )
    private String name;

    @Column( name = "description" )
    private String description;

    @Column( name = "currency", nullable = false )
    private String currency;

    @Column( name = "created_date" )
    private OffsetDateTime createdDate;

    @Column( name = "last_updated_date" )
    private OffsetDateTime lastUpdatedDate;

}
