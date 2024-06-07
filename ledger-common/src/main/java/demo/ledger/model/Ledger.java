package demo.ledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table( name = "ledger" )
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ledger {

    @Id
    @GeneratedValue
    private long id;

    @Column( name = "uuid", nullable = false )
    private String uuid;

    @Column( name = "name", nullable = false )
    private String name;

    @Column( name = "description" )
    private String description;

    @Column( name = "created_date" )
    private OffsetDateTime createdDate;

    @Column( name = "last_updated_date" )
    private OffsetDateTime lastUpdatedDate;

}
