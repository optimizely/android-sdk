/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.fsc_app.bdd.support;

import android.content.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.event.internal.payload.Snapshot;
import com.optimizely.ab.event.internal.payload.Visitor;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.optlyplugins.TestCompositeService;
import com.optimizely.ab.fsc_app.bdd.models.ApiOptions;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.optimizely.ab.fsc_app.bdd.models.Constants.DISPATCHED_EVENTS;
import static com.optimizely.ab.fsc_app.bdd.models.Constants.USER_PROFILES;
import static com.optimizely.ab.fsc_app.bdd.support.ResponseComparator.compareResults;
import static com.optimizely.ab.fsc_app.bdd.support.Utils.copyResponse;
import static com.optimizely.ab.fsc_app.bdd.support.Utils.parseYAML;
import static junit.framework.TestCase.fail;

public class Steps {

    private Context context = getInstrumentation().getTargetContext();
    private ApiOptions apiOptions;
    private OptimizelyWrapper optimizelyWrapper;
    private BaseResponse result;
    private List<BaseResponse> resultsList;

    @Before
    public void setup() {
        apiOptions = new ApiOptions(context);
        optimizelyWrapper = new OptimizelyWrapper();
        result = null;
    }

    @After
    public void tearDown() {
        apiOptions = null;
        OptlyDataHelper.projectConfig = null;
    }

    @Given("^the datafile is \"(\\S+)*\"$")
    public void the_datafile_is(String datafileName) {
        apiOptions.setDatafile(datafileName);
        OptlyDataHelper.initializeProjectConfig(apiOptions.getDatafile());
        Assert.assertNotNull(datafileName);
    }

    @Given("^the User Profile Service is \"(\\S+)*\"$")
    public void user_profile_service_is(String userProfile) {
        apiOptions.setUserProfileService(userProfile);
        Assert.assertNotNull(userProfile);
    }

    @Given("^(\\d+) \"(\\S+)*\" listener is added$")
    public void listener_is_added(int count, String listenerType) {
        HashMap map = new HashMap<String, Object>();
        map.put("count", count);
        map.put("type", listenerType);
        apiOptions.addWithListener(map);
    }

    @Given("^the event processor configuration is$")
    public void theEventProcessorConfigurationIs(String args) {
        apiOptions.setEventOptions((Map<String, Object>) parseYAML(args));
    }

    @Given("^the response is delayed by (\\d+\\.\\d+) times the event_options (\\S+)$")
    public void response_delayed_by_times(Double responseDelay, String fieldName) {
        apiOptions.setResponseDelay((int) (Double.parseDouble(apiOptions.getEventOptions().get(fieldName).toString()) * responseDelay));
    }

    @Given("^requests are made in the same session$")
    public void requestsAreMadeInTheSameSession() {
        apiOptions.setSessionId(UUID.randomUUID().toString());
    }

    @When("^(\\S+) is called with arguments$")
    public void is_called_with_arguments(String api, String args) {
        apiOptions.setApi(api);
        apiOptions.setArguments(args);
        result = optimizelyWrapper.callApi(apiOptions);
    }

    @When("^the following calls are made$")
    public void theFollowingCallsAreMade(String args) {
        resultsList = new ArrayList<>();
        ArrayList<Map<String, String>> calls = (ArrayList<Map<String, String>>) parseYAML(args);
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (Map<String, String> call : calls) {
                apiOptions.setApi(call.get("method"));
                apiOptions.setArguments(mapper.writeValueAsString(call.get("args")));
                result = optimizelyWrapper.callApi(apiOptions);
                resultsList.add(result);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Then("^the result should be (\\d+)$")
    public void then_result_should_be_int(Integer expectedValue) {
        Assert.assertTrue(result.compareResults(expectedValue));
    }

    @Then("^the result should be (\\d+\\.\\d+)$")
    public void then_result_should_be_double(Double expectedValue) {
        Assert.assertTrue(result.compareResults(expectedValue));
    }

    @Then("^the result should be \"([^\"]*)\"$")
    public void then_result_should_be_quoted_string(Object expectedValue) {
        Assert.assertTrue(result.compareResults(expectedValue));
    }

    @Then("^the result should match list \"([^\"]*)\"$")
    public void the_result_should_match_list(String response) {
        List<String> resultsArray = new ArrayList<>();
        if (response.equals("[]")) {
            Assert.assertTrue(result.compareResults(resultsArray));
        } else {
            resultsArray = Arrays.asList(response.split(","));
            Assert.assertTrue(result.compareResults(resultsArray));
        }
    }

    @Then("^in the response, \"([^\"]*)\" should have each one of these$")
    public void in_the_response_should_have_each_one_of_these(String type, String args) {
        Assert.assertTrue(compareResults(type,
                parseYAML(args),
                result));
    }

    @Then("^the result should be boolean \"(\\S+)*\"$")
    public void then_result_type_should_be_boolean(final Object expectedValue) {
        Assert.assertTrue(result.compareResults(Boolean.parseBoolean(expectedValue.toString().toLowerCase())));
    }

    @Then("^in the response, \"(\\S+)*\" should be \"(\\S+)*\"$")
    public void then_in_the_response(String field, String args) {
        Assert.assertTrue(compareResults(field,
                parseYAML(args),
                result));
    }

    @Then("^in the response, \"(\\S+)*\" should match$")
    public void then_response_should_match(String field, String args) {
        Assert.assertTrue(compareResults(field,
                parseYAML(args),
                result));
    }

    @Then("^the result should be the array$")
    public void theResultShouldBeTheArray(String args) {
        List<String> responseList = (List<String>) parseYAML(args);
        for (int i = 0; i < responseList.size(); i++) {
            Assert.assertTrue(resultsList.get(i).compareResults(responseList.get(i)));
        }
    }

    @Then("^dispatched events payloads include$")
    public void then_dispatched_event_payload_include(String args) {
        Assert.assertTrue(compareResults(DISPATCHED_EVENTS,
                parseYAML(args),
                apiOptions.getDispatchedEvents()));
    }

    @Then("^there are no dispatched events$")
    public void then_no_dispatched_event() {
        Assert.assertTrue(apiOptions.getDispatchedEvents().isEmpty());
    }

    @Then("^in the response, the \"([^\"]*)\" listener was called (\\d+) times$")
    public void in_the_response_the_listener_was_called_times(String type, int count, String args) {
        Assert.assertTrue(compareResults(type,
                copyResponse(count, parseYAML(args)),
                result));
    }

    @Then("^the User Profile Service state should be$")
    public void the_User_Profile_Service_state_should_be(String args) {
        Assert.assertTrue(compareResults(USER_PROFILES,
                parseYAML(args),
                OptlyDataHelper.getUserProfiles(optimizelyWrapper.getOptimizelyManager())));
    }

    @Given("^user \"([^\"]*)\" has mapping \"([^\"]*)\": \"([^\"]*)\" in User Profile Service$")
    public void user_has_mapping_in_User_Profile_Service(String userName, String experimentKey, String variationKey) {
        apiOptions.addUserProfile(userName, experimentKey, variationKey);
    }

    @Then("^there is no user profile state$")
    public void there_is_no_user_profile_state() {
        Assert.assertTrue(TestCompositeService.getUserProfiles(optimizelyWrapper.getOptimizelyManager()).isEmpty());
    }

    @Then("^the number of dispatched events is (\\d+)$")
    public void the_number_of_dispatched_events_is(int expectedCount) {
        try {
            // BatchEventProcessor in android-sdk sleeps for 50ms if there are no events to process but in order to handle multiple async responses we have to wait 1500 ms
            // That time is taken into account here in order for the new events in the queue to be recognized before sending the response
            if (apiOptions.getEventOptions() != null) {
                Thread.sleep(500);
            }
            if (apiOptions.getResponseDelay() != null) {
                Thread.sleep(apiOptions.getResponseDelay());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            Assert.assertEquals(expectedCount, apiOptions.getDispatchedEvents().size());
        }
    }

    @Then("^payloads of dispatched events don't include decisions$")
    public void payloads_of_dispatched_events_dont_include_decisions() {
        Assert.assertTrue(checkNoDecision());
    }

    @Then("^in the response, \"([^\"]*)\" should have this exactly (\\d+) times$")
    public void in_the_response_should_have_this_exactly_times(String field, int count, String args) {
        Assert.assertTrue(compareResults(field,
                copyResponse(count, parseYAML(args)),
                result));
    }

    private Boolean checkNoDecision() {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> dispatchEvent = apiOptions.getDispatchedEvents();
        if (dispatchEvent == null || dispatchEvent.size() == 0) {
            fail("No events returned");
            return false;
        }

        for (Map<String, Object> objMap : dispatchEvent) {
            Assert.assertTrue(objMap.get("params") instanceof Map);
            EventBatch batch = mapper.convertValue(objMap.get("params"), EventBatch.class);
            for (Visitor visitor : batch.getVisitors()) {
                for (Snapshot snapshot : visitor.getSnapshots()) {
                    if (snapshot.getDecisions() == null)
                        return true;
                }
            }
        }
        return false;
    }

    @Then("^dispatched event at index (\\d+) visitors payload includes$")
    public void dispatchedEventAtIndex(int index, String payload) {
        Assert.assertTrue((apiOptions.getDispatchedEvents().size() - 1) >= index);
        List visitorsExpected = (List) parseYAML(payload);
        List visitorsActual = (List) ((Map) apiOptions.getDispatchedEvents().get(index).get("params")).get("visitors");
        Assert.assertTrue(Utils.containsSubset(visitorsExpected, visitorsActual));
    }
}
