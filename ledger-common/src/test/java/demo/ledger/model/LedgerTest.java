package demo.ledger.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LedgerTest {

    @Test
    public void testLedgerSettersGetters() {
        Ledger ledger = new Ledger();
        ledger.setId( 12345L );
        ledger.setName( "My First Ledger" );
        assertThat( ledger.getId(), is( 12345L ) );
        assertThat( ledger.getName(), is( "My First Ledger" ) );
    }
}
