package com.optimizely.ab.android.test_app.bdd.support;

import android.content.Context;

import com.optimizely.ab.android.test_app.bdd.support.resources.BaseResource;

import org.junit.Assert;

import java.util.HashMap;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

public class Steps {

    private Context context = getInstrumentation().getTargetContext();
    private OptimizelyWrapper optimizelyWrapper;

    @Before
    public void setup() {
        optimizelyWrapper = new OptimizelyWrapper(context);
    }

    @After
    public void tearDown() {
        optimizelyWrapper = null;
    }


    @Given("^the datafile is \"(\\S+)*\"$")
    public void the_datafile_is(String datafileName) {
        optimizelyWrapper.setDatafile(datafileName);
        Assert.assertNotNull(datafileName);
    }

    @Given("^(\\d+) \"(\\S+)*\" listener is added$")
    public void listener_is_added(int count, String listenerType) {
        HashMap map = new HashMap<String, Object>();
        map.put("count", count);
        map.put("type", listenerType);
        optimizelyWrapper.addWithListener(map);
    }

    @When("^(\\S+) is called with arguments$")
    public void is_called_with_arguments(String api, String args) {
        optimizelyWrapper.callApi(api, args);
        Assert.assertNotNull(api);
    }

    @Then("^the result should be \"([^\"]*)\"$")
    public void then_result_should_be(Object expectedValue) {
        Assert.assertTrue(optimizelyWrapper.getResult().compareResults(expectedValue));
    }

    @Then("^in the response, \"(\\S+)*\" should be \"(\\S+)*\"$")
    public void then_in_the_response(String field, String args) {
        Assert.assertTrue(optimizelyWrapper.compareFields(field, args));
    }

    @Then("^in the response, \"(\\S+)*\" should match$")
    public void then_response_should_match(String field, String args) {
        Assert.assertTrue(optimizelyWrapper.compareFields(field, args));
    }

    @Then("^dispatched events payloads include$")
    public void then_dispatched_event_payload_include(String args) {
        Assert.assertTrue(optimizelyWrapper.compareFields("dispatch_event", args));
    }

    @Then("^there are no dispatched events$")
    public void then_no_dispatched_event() {
        Assert.assertTrue(optimizelyWrapper.getDispatchedEvents().isEmpty());
    }

    @Then("^in the response, the \"([^\"]*)\" listener was called (\\d+) times$")
    public void in_the_response_the_listener_was_called_times(String type, int count) {
        // TODO: Write code here that turns the phrase above into concrete actions
    }

    @Then("^the User Profile Service state should be$")
    public void the_User_Profile_Service_state_should_be(String arg1) throws Throwable {
        // TODO: Write code here that turns the phrase above into concrete actions
    }

    @Then("^there is no user profile state$")
    public void there_is_no_user_profile_state() throws Throwable {
        Assert.assertTrue(BaseResource.getUserProfiles(optimizelyWrapper.getOptimizelyManager()).isEmpty());
    }

    @Then("^the number of dispatched events is (\\d+)$")
    public void the_number_of_dispatched_events_is(int count) throws Throwable {
        // TODO: Write code here that turns the phrase above into concrete actions
    }
}
