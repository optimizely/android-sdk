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

package com.optimizely.ab.integration_test.app.optlyplugins.userprofileservices;

import com.optimizely.ab.bucketing.UserProfileService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NormalService implements UserProfileService, TestUserProfileService {
    public Map<String, Map<String, Object>> userProfiles = new HashMap<>();

    public NormalService(ArrayList<Map> userProfileList) {
        if (userProfileList != null) {
            for (Map userProfile : userProfileList) {
                String userId = userProfile.get("user_id").toString();
                userProfiles.put(userId, userProfile);
            }
        }
    }

    public NormalService() {}

    public Map<String, Object> lookup(String userId) throws Exception {
        return userProfiles.get(userId);
    }

    public void save(Map<String, Object> userProfile) throws Exception {
        String userId = userProfile.get("user_id").toString();
        userProfiles.put(userId, userProfile);
    }

    public Collection<Map<String,Object>> getUserProfiles() {
        return userProfiles.values();
    }
}
