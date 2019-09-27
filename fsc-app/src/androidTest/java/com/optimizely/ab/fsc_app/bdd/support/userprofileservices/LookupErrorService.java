package com.optimizely.ab.fsc_app.bdd.support.userprofileservices;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class LookupErrorService extends NormalService {
    public LookupErrorService(ArrayList<LinkedHashMap> userProfileList) {
        super(userProfileList);
    }

    public Map<String, Object> lookup(String userId) throws Exception {
        throw new Exception("LookupError");
    }
}
