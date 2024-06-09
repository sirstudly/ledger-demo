package demo.ledger;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines( "cucumber" )
@SelectPackages( "demo.ledger" )
@ConfigurationParameter( key = PLUGIN_PROPERTY_NAME, value = "pretty" )
public class RunCucumberTests {
}
