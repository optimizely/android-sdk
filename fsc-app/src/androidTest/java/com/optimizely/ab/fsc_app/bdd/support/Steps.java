package com.optimizely.ab.fsc_app.bdd.support;

import android.content.Context;

import com.optimizely.ab.fsc_app.bdd.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.support.customeventdispatcher.ProxyEventDispatcher;
import com.optimizely.ab.fsc_app.bdd.support.requests.OptimizelyRequest;
import com.optimizely.ab.fsc_app.bdd.support.resources.BaseResource;

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
    private OptimizelyRequest optimizelyRequest;
    private OptimizelyE2EService  optimizelyE2EService;

    @Before
    public void setup() {
        optimizelyRequest = new OptimizelyRequest(context);
        optimizelyE2EService = new OptimizelyE2EService();
    }

    @After
    public void tearDown() {
        optimizelyRequest = null;
    }

    @Given("^the datafile is \"(\\S+)*\"$")
    public void the_datafile_is(String datafileName) {
        optimizelyRequest.setDatafile(datafileName);
        Assert.assertNotNull(datafileName);
    }

    @Given("^(\\d+) \"(\\S+)*\" listener is added$")
    public void listener_is_added(int count, String listenerType) {
        HashMap map = new HashMap<String, Object>();
        map.put("count", count);
        map.put("type", listenerType);
        optimizelyRequest.addWithListener(map);
    }

    @When("^(\\S+) is called with arguments$")
    public void is_called_with_arguments(String api, String args) {
        optimizelyRequest.setApi(api);
        optimizelyRequest.setArguments(args);
        optimizelyE2EService.callApi(optimizelyRequest);
        Assert.assertNotNull(api);
    }

    @Then("^the result should be \"([^\"]*)\"$")
    public void then_result_should_be(Object expectedValue) {
        Assert.assertTrue(optimizelyE2EService.getResult().compareResults(expectedValue));
    }

    @Then("^in the response, \"(\\S+)*\" should be \"(\\S+)*\"$")
    public void then_in_the_response(String field, String args) {
        Assert.assertTrue(optimizelyE2EService.compareFields(field, args));
    }

    @Then("^in the response, \"(\\S+)*\" should match$")
    public void then_response_should_match(String field, String args) {
        Assert.assertTrue(optimizelyE2EService.compareFields(field, args));
    }

    @Then("^dispatched events payloads include$")
    public void then_dispatched_event_payload_include(String args) {
        Assert.assertTrue(optimizelyE2EService.compareFields("dispatched_event", args));
    }

    @Then("^there are no dispatched events$")
    public void then_no_dispatched_event() {
        Assert.assertTrue(ProxyEventDispatcher.getDispatchedEvents().isEmpty());
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
        Assert.assertTrue(BaseResource.getUserProfiles(optimizelyE2EService.getOptimizelyManager()).isEmpty());
    }

    @Then("^the number of dispatched events is (\\d+)$")
    public void the_number_of_dispatched_events_is(int count) throws Throwable {
        // TODO: Write code here that turns the phrase above into concrete actions
    }
}
