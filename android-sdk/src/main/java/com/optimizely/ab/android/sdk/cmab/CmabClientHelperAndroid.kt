// Copyright 2025, Optimizely, Inc. and contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.optimizely.ab.android.sdk.cmab

import com.optimizely.ab.cmab.client.CmabClientHelper

open class CmabClientHelperAndroid {

    open var cmabPredictionEndpoint: String = CmabClientHelper.CMAB_PREDICTION_ENDPOINT

    open val cmabFetchFailed: String
        get() = CmabClientHelper.CMAB_FETCH_FAILED

    open val invalidCmabFetchResponse: String
        get() = CmabClientHelper.INVALID_CMAB_FETCH_RESPONSE

    open fun buildRequestJson(
        userId: String?,
        ruleId: String?,
        attributes: Map<String?, Any?>?,
        cmabUuid: String?
    ): String {
        return CmabClientHelper.buildRequestJson(userId, ruleId, attributes, cmabUuid)
    }

    open fun parseVariationId(jsonResponse: String?): String? {
        return CmabClientHelper.parseVariationId(jsonResponse)
    }

    open fun validateResponse(responseBody: String?): Boolean {
        return CmabClientHelper.validateResponse(responseBody)
    }

    open fun isSuccessStatusCode(statusCode: Int): Boolean {
        return CmabClientHelper.isSuccessStatusCode(statusCode)
    }
}
