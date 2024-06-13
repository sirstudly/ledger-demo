package demo.ledger.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import demo.ledger.model.LedgerTransaction;

import java.lang.reflect.Type;

/**
 * Required because we get the following error when trying to serialize using Gson (probably because of the 1-to-many mapping.:
 * * java.lang.UnsupportedOperationException: Attempted to serialize java.lang.Class: org.hibernate.proxy.HibernateProxy.
 */
public class LedgerTransactionConverter implements JsonSerializer<LedgerTransaction> {

    @Override
    public JsonElement serialize( LedgerTransaction txn, Type type, JsonSerializationContext jsonSerializationContext ) {
        JsonObject obj = new JsonObject();
        obj.addProperty( "id", txn.getId() );
        obj.addProperty( "uuid", txn.getUuid() );
        obj.addProperty( "description", txn.getDescription() );
//        obj.add( "ledgerEntries", txn.getLedgerEntries() );
//        obj.add( "createdDate", txn.getCreatedDate() );
        return obj;
    }

}
