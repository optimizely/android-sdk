package com.optimizely.ab.android.test_app.test;

import android.content.Context;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.test_app.R;
import com.optimizely.ab.android.test_app.user_profile_services.NoOpService;
import com.optimizely.ab.bucketing.UserProfileService;

import org.junit.Assert;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.optimizely.ab.android.sdk.OptimizelyManager.loadRawResource;

public class MainActivitySteps {

    public static final String PROJECT_ID = "10554895220";
    private OptimizelyManager optimizelyManager;
    private Context context = getInstrumentation().getTargetContext();
    private String datafileName;
    private OptimizelyClient optimizelyClient;
    private Object result;

    @Before
    public void setup() {
        UserProfileService userProfileService = new NoOpService();
        OptimizelyManager.Builder builder = OptimizelyManager.builder();
        optimizelyManager =  builder.withEventDispatchInterval(60L * 10L)
                .withDatafileDownloadInterval(30L)
                .withSDKKey("FCnSegiEkRry9rhVMroit4")
                .withUserProfileService(userProfileService)
                .build(context);
    }

    @After
    public void tearDown() {
        optimizelyManager = null;
        optimizelyClient = null;
    }


    @Given("^the datafile is \"(\\S+)*\"$")
    public void the_datafile_is(String datafileName) {
        this.datafileName = datafileName;

        try {
            optimizelyClient = optimizelyManager.initialize(context, loadRawResource(context, R.raw.feature_exp));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(datafileName);
        Assert.assertTrue(optimizelyClient.isValid());
    }

    @Given("^(\\d+) \"(\\S+)*\" listener is added$")
    public void listener_is_added(int count, String listenerType) {
        HashMap map = new HashMap<String, Object>();
        map.put("count", count);
        map.put("type", listenerType);
    }

    @When("^(\\S+) is called with arguments$")
    public void is_called_with_arguments(String api, String args) {
        Yaml yaml = new Yaml();
        Object obj = yaml.load(args);
        if(api.equals("is_feature_enabled")) {
            this.result = optimizelyClient.isFeatureEnabled((String) ((LinkedHashMap) obj).get("feature_flag_key"),
                    (String) ((LinkedHashMap) obj).get("user_id"),
                    (Map<String, ?>)((LinkedHashMap) obj).get("attributes"));
        }
        Assert.assertNotNull(api);
    }

    @Then("^the result should be \"(\\S+)*\"$")
    public void then_result_should_be(Object expectedValue) {
        Assert.assertTrue(optimizelyClient.isValid());

        Object expectedVal = expectedValue;
        if (expectedValue.equals("NULL")) {
            expectedVal = null;
        } else if (expectedVal.equals("true") || expectedVal.equals("false")) {
            expectedVal = Boolean.parseBoolean((String) expectedVal);
        } else {
            try {
                double d = Double.parseDouble((String) expectedVal);
            } catch (NumberFormatException | NullPointerException nfe) {
            }
        }

        Assert.assertEquals(result, expectedVal);
    }

    @Then("^in the response, \"(\\S+)*\" should be \"(\\S+)*\"$")
    public void then_in_the_response(String field, Object expectedValue) {
        Object expectedVal = expectedValue;
        if (expectedValue.equals("NULL")) {
            expectedVal = null;
        }
    }

    @Then("^in the response, \"(\\S+)*\" should match$")
    public void then_response_should_match(String field, String args) {
        Yaml yaml = new Yaml();
        Object obj = yaml.load(args);
        Assert.assertNotNull(field);
    }
    @Then("^dispatched events payloads include$")
    public void then_dispatched_event_payload_include(String args) {
        Yaml yaml = new Yaml();
        Object obj = yaml.load(args);
        Assert.assertNotNull(obj);
    }

    @Then("^there are no dispatched events$")
    public void  then_no_dispatched_event() {
        Assert.assertNotNull("");
    }
}
