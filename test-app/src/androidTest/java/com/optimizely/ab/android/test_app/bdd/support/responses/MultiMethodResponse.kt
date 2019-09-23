package com.optimizely.ab.android.test_app.bdd.support.responses

import com.fasterxml.jackson.annotation.JsonProperty

data class MultiMethodResponse(
        @JsonProperty("result_1")
        public var result_1: String?,
        @JsonProperty("result_2")
        public var result_2: String?,
        @JsonProperty("result_3")
        public var result_3: String?,
        @JsonProperty("result_4")
        public var result_4: String?
)