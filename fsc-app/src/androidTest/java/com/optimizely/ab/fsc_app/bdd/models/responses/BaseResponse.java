package com.optimizely.ab.fsc_app.bdd.models.responses;


public interface BaseResponse {
    Boolean compareResults(Object expectedResponse);
}
