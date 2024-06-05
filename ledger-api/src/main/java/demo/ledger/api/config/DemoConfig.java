package demo.ledger.api.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;

@Configuration
public class DemoConfig {

    @Bean
    public Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter( OffsetDateTime.class, new OffsetDateTimeConverter() );
        builder.setPrettyPrinting();
        return builder.create();
    }
}
