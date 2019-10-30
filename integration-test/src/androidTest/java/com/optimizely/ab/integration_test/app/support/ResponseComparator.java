package com.optimizely.ab.integration_test.app.support;

import com.optimizely.ab.integration_test.app.models.responses.BaseListenerMethodResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.optimizely.ab.integration_test.app.models.Constants.DISPATCHED_EVENTS;
import static com.optimizely.ab.integration_test.app.models.Constants.LISTENER_CALLED;
import static com.optimizely.ab.integration_test.app.models.Constants.USER_PROFILES;

public class ResponseComparator {
    private static Logger logger = LoggerFactory.getLogger(ResponseComparator.class);

    public static Boolean compareResults(String field, Object expectedResult, Object actualResult) {
        switch (field) {
            case LISTENER_CALLED:
                return compareListenerCalled(expectedResult, actualResult);
            case DISPATCHED_EVENTS:
                return compareDispatchedEvents(expectedResult, actualResult);
            case USER_PROFILES:
                try {
                    return Utils.containsSubset((ArrayList) expectedResult, (ArrayList) actualResult);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    return false;
                }
            default:
                return false;
        }
    }

    private static Boolean compareDispatchedEvents(Object expectedResult, Object actualResult) {
        try {
            // This checks if both values are equal and this case will only be true when both are null
            if(expectedResult == null || actualResult == null){
                return expectedResult == actualResult;
            }
            ArrayList<HashMap> actualDispatchedEvents = (ArrayList<HashMap>) actualResult;
            ArrayList expectedDispatchedEvents = (ArrayList) expectedResult;
            for (int i = 0; i < expectedDispatchedEvents.size(); i++) {
                HashMap actualParams = (HashMap) actualDispatchedEvents.get(i).get("params");
                HashMap expectedParams = (HashMap) expectedDispatchedEvents.get(i);
                if (!Utils.containsSubset(expectedParams, actualParams)) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    private static Boolean compareListenerCalled(Object expectedResult, Object actualResult) {
        BaseListenerMethodResponse baseListenerMethodResponse;
        if (actualResult instanceof BaseListenerMethodResponse)
            baseListenerMethodResponse = (BaseListenerMethodResponse) actualResult;
        else
            return false;

        if (expectedResult == baseListenerMethodResponse.getListenerCalled()) {
            return true;
        }

        try {
            List actualListenersCalled = new ArrayList(baseListenerMethodResponse.getListenerCalled());
            baseListenerMethodResponse.getListenerCalled().clear();
            return Utils.containsSubset((List) expectedResult, actualListenersCalled);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return actualResult == baseListenerMethodResponse.getListenerCalled();
    }
}
