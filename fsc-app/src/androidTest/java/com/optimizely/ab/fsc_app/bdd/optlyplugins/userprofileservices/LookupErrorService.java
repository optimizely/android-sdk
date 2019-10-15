package com.optimizely.ab.fsc_app.bdd.optlyplugins.userprofileservices;

import java.util.ArrayList;
import java.util.Map;

public class LookupErrorService extends NormalService {
    public LookupErrorService(ArrayList<Map> userProfileList) {
        super(userProfileList);
    }

    public Map<String, Object> lookup(String userId) throws Exception {
        throw new Exception("LookupError");
    }
}
