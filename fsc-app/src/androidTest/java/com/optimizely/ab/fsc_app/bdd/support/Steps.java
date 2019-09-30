package com.optimizely.ab.fsc_app.bdd.support;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.fsc_app.bdd.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.support.customeventdispatcher.ProxyEventDispatcher;
import com.optimizely.ab.fsc_app.bdd.support.requests.OptimizelyRequest;
import com.optimizely.ab.fsc_app.bdd.support.resources.BaseResource;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.fail;

public class Steps {

    private Context context = getInstrumentation().getTargetContext();
    private OptimizelyRequest optimizelyRequest;
    private OptimizelyE2EService optimizelyE2EService;

    @Before
    public void setup() {
        optimizelyRequest = new OptimizelyRequest(context);
        optimizelyE2EService = new OptimizelyE2EService();
    }

    @After
    public void tearDown() {
        optimizelyRequest = null;
        Utils.projectConfig = null;
    }

    @Given("^the datafile is \"(\\S+)*\"$")
    public void the_datafile_is(String datafileName) {
        optimizelyRequest.setDatafile(datafileName);
        Utils.initializeProjectConfig(optimizelyRequest.getDatafile());
        Assert.assertNotNull(datafileName);
    }

    @Given("^the User Profile Service is \"(\\S+)*\"$")
    public void user_profile_service_is(String userProfile) {
        optimizelyRequest.setUserProfileService(userProfile);
        Assert.assertNotNull(userProfile);
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
        Assert.assertTrue(optimizelyE2EService.compareFields(field, 1, args));
    }

    @Then("^in the response, \"(\\S+)*\" should match$")
    public void then_response_should_match(String field, String args) {
        Assert.assertTrue(optimizelyE2EService.compareFields(field, 1, args));
    }

    @Then("^dispatched events payloads include$")
    public void then_dispatched_event_payload_include(String args) {
        Assert.assertTrue(optimizelyE2EService.compareFields("dispatched_event", 1, args));
    }

    @Then("^there are no dispatched events$")
    public void then_no_dispatched_event() {
        Assert.assertTrue(ProxyEventDispatcher.getDispatchedEvents().isEmpty());
    }

    @Then("^in the response, the \"([^\"]*)\" listener was called (\\d+) times$")
    public void in_the_response_the_listener_was_called_times(String type, int count, String args) {
        Assert.assertTrue(optimizelyE2EService.compareFields(type, count, args));
    }

    @Then("^the User Profile Service state should be$")
    public void the_User_Profile_Service_state_should_be(String arg1) throws Throwable {
        // TODO: Write code here that turns the phrase above into concrete actions
    }

    @Given("^user \"([^\"]*)\" has mapping \"([^\"]*)\": \"([^\"]*)\" in User Profile Service$")
    public void user_has_mapping_in_User_Profile_Service(String userName, String experimentKey, String variationKey) throws Throwable {
        Experiment experiment = Utils.getExperimentByKey(experimentKey);
        String experimentId = "invalid_experiment";
        if(experiment != null) {
            experimentId = experiment.getId();
        }
        Variation variation = Utils.getVariationByKey(experiment, variationKey);
        String variationId = "invalid_variation";
        if(variation != null) {
            variationId = variation.getId();
        }

        Map<String, Object> userProfile = new HashMap<>();
        boolean foundMap = false;
        for(Map userProfileMap: optimizelyRequest.getUserProfiles()) {
            if(userProfileMap.containsValue(userName)) {
                foundMap = true;
                userProfile = userProfileMap;
                optimizelyRequest.getUserProfiles().remove(userProfileMap);
            }
        }
        Map<String, Object> expBucketMap = new HashMap<>();
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("variation_id", variationId);
        expBucketMap.put(experimentId, varMap);

        if(!foundMap) {
            userProfile.put("user_id", userName);
        } else {
            expBucketMap = (Map<String, Object>) userProfile.get("experiment_bucket_map");
            expBucketMap.put(experimentId, varMap);
        }
        userProfile.put("experiment_bucket_map", expBucketMap);

        optimizelyRequest.addUserProfile(userProfile);
    }

    @Then("^there is no user profile state$")
    public void there_is_no_user_profile_state() throws Throwable {
        Assert.assertTrue(BaseResource.getUserProfiles(optimizelyE2EService.getOptimizelyManager()).isEmpty());
    }

    @Then("^the number of dispatched events is (\\d+)$")
    public void the_number_of_dispatched_events_is(int count) throws Throwable {
        Assert.assertSame(count, ProxyEventDispatcher.getDispatchedEvents().size());
    }

    @Then("^payloads of dispatched events don't include decisions$")
    public void payloads_of_dispatched_events_dont_include_decisions() throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<Map<String, Object>> dispatchEvent = ProxyEventDispatcher.getDispatchedEvents();
        if (dispatchEvent == null || dispatchEvent.size() == 0) {
            fail("No events returned");
        }

        for (Map<String, Object> objMap : dispatchEvent) {
            Assert.assertTrue(objMap.get("params") instanceof Map);
            EventBatch batch = mapper.convertValue(objMap.get("params"), EventBatch.class);
            for (Visitor visitor : batch.getVisitors()) {
                for (Snapshot snapshot : visitor.getSnapshots()) {
                    Assert.assertNull(snapshot.getDecisions());
                }
            }
        }
    }

    @Then("^in the response, \"([^\"]*)\" should have this exactly (\\d+) times$")
    public void in_the_response_should_have_this_exactly_times(String field, int count, String args) throws Throwable {
        Assert.assertTrue(optimizelyE2EService.compareFields(field, count, args));
    }
}
