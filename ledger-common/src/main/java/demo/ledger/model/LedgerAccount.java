package demo.ledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table( name = "ledger_account" )
public class LedgerAccount {

    @Id
    @GeneratedValue
    private Long id;
    @Column( name = "uuid", nullable = false, unique = true )
    private String uuid;
    @Column( name = "name", nullable = false )
    private String name;
    @Column( name = "description" )
    private String description;
    @Column( name = "currency", nullable = false )
    private String currency;
    @Column( name = "ledgerUuid", nullable = false )
    private String ledgerUuid;

    public LedgerAccount( Long id, String uuid, String name, String description, String currency, String ledgerUuid ) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.id = id;
        this.currency = currency;
        this.ledgerUuid = ledgerUuid;
    }

    public LedgerAccount() {
        // empty constructor
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid( String uuid ) {
        this.uuid = uuid;
    }

    public LedgerAccount withUuid( String uuid ) {
        this.uuid = uuid;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency( String currency ) {
        this.currency = currency;
    }

    public String getLedgerUuid() {
        return ledgerUuid;
    }

    public void setLedgerUuid( String ledgerUuid ) {
        this.ledgerUuid = ledgerUuid;
    }
}
