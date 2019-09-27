package com.optimizely.ab.fsc_app.bdd.support.userprofileservices;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class SaveErrorService extends NormalService {
    public SaveErrorService(ArrayList<LinkedHashMap> userProfileList) {
        super(userProfileList);
    }

    public void save(Map<String, Object> userProfile) throws Exception {
        throw new Exception("SaveError");
    }
}
