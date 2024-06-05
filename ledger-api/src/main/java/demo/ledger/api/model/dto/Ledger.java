package demo.ledger.api.model.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger")
public class Ledger {

    @Id
    @GeneratedValue
    private Long id;
    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "description")
    private String description;

    public Ledger( String uuid, String name, String description ) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
    }

    public Ledger( Long id, String uuid, String name, String description ) {
        this( uuid, name, description );
        this.id = id;
    }

    public Ledger() {
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

    public Ledger withUuid(String uuid) {
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
}
