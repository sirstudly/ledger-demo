package demo.ledger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.net.http.HttpClient;

@Configuration
@EnableAutoConfiguration( exclude = {DataSourceAutoConfiguration.class} )
public class TestConfiguration {

    @Bean
    public Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        return builder.create();
    }

    @Bean
    public HttpClient getHttpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
//    @Scope( ConfigurableBeanFactory.SCOPE_PROTOTYPE )
    public CucumberTestState getCucumberTestState( HttpClient httpClient, Gson gson, @Value( value = "${demo.ledger.restserver.baseurl}" ) String baseUrl ) {
        return new CucumberTestState( httpClient, gson, baseUrl );
    }
}
