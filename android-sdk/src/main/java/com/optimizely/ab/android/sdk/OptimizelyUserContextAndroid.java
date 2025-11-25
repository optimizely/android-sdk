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

package com.optimizely.ab.android.sdk;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyForcedDecision;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelydecision.OptimizelyDecisionCallback;;
import com.optimizely.ab.optimizelydecision.OptimizelyDecisionsCallback;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// This class extends OptimizelyUserContext from the Java-SDK core to maintain backward compatibility
// with synchronous decide API calls. It ensures proper functionality for legacy implementations
// that rely on synchronous behavior, while excluding feature flags that require asynchronous decisions.

public class OptimizelyUserContextAndroid extends OptimizelyUserContext {

    /**
     * Creates an Android user context with basic parameters.
     *
     * @param optimizely The Optimizely client instance
     * @param userId Unique identifier for the user
     * @param attributes Map of user attributes for targeting and segmentation
     */
    public OptimizelyUserContextAndroid(@NonNull Optimizely optimizely,
                                        @NonNull String userId,
                                        @NonNull Map<String, ?> attributes) {
        super(optimizely, userId, attributes);
    }

    /**
     * Creates an Android user context with forced decisions and qualified segments.
     *
     * @param optimizely The Optimizely client instance
     * @param userId Unique identifier for the user
     * @param attributes Map of user attributes for targeting and segmentation
     * @param forcedDecisionsMap Map of forced decisions to override normal flag evaluation
     * @param qualifiedSegments List of audience segments the user qualifies for
     */
    public OptimizelyUserContextAndroid(@NonNull Optimizely optimizely,
                                        @NonNull String userId,
                                        @NonNull Map<String, ?> attributes,
                                        @Nullable Map<String, OptimizelyForcedDecision> forcedDecisionsMap,
                                        @Nullable List<String> qualifiedSegments) {
        super(optimizely, userId, attributes, forcedDecisionsMap, qualifiedSegments);
    }

    /**
     * Creates an Android user context with all available parameters including analytics control.
     *
     * @param optimizely The Optimizely client instance
     * @param userId Unique identifier for the user
     * @param attributes Map of user attributes for targeting and segmentation
     * @param forcedDecisionsMap Map of forced decisions to override normal flag evaluation
     * @param qualifiedSegments List of audience segments the user qualifies for
     * @param shouldIdentifyUser Whether to send user identification events for analytics
     */

    public OptimizelyUserContextAndroid(@NonNull Optimizely optimizely,
                                 @NonNull String userId,
                                 @NonNull Map<String, ?> attributes,
                                 @Nullable Map<String, OptimizelyForcedDecision> forcedDecisionsMap,
                                 @Nullable List<String> qualifiedSegments,
                                 @Nullable Boolean shouldIdentifyUser) {
        super(optimizely, userId, attributes, forcedDecisionsMap, qualifiedSegments, shouldIdentifyUser);
    }

    // ===========================================
    // [SYNCHRONOUS DECIDE METHODS]
    // - "decideSync" methods skip async decision like cmab for backward compatibility.
    // ===========================================

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     * <ul>
     * <li>If the SDK finds an error, it'll return a decision with <b>null</b> for <b>variationKey</b>. The decision will include an error message in <b>reasons</b>.
     * </ul>
     * <p>
     * Note: This API is specifically designed for synchronous decision-making only.
     * For asynchronous decision-making, use the decideAsync() API.
     * </p>
     * @param key A flag key for which a decision will be made.
     * @param options A list of options for decision-making.
     * @return A decision result.
     */
    @Override
    public OptimizelyDecision decide(@NonNull String key,
                                     @NonNull List<OptimizelyDecideOption> options) {
        return coreDecideSync(key, options);
    }

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     *
     * <p>
     * Note: This API is specifically designed for synchronous decision-making only.
     * For asynchronous decision-making, use the decideAsync() API.
     * </p>
     * @param key A flag key for which a decision will be made.
     * @return A decision result.
     */
    @Override
    public OptimizelyDecision decide(@NonNull String key) {
        return coreDecideSync(key, Collections.emptyList());
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for multiple flag keys and a user context.
     * <ul>
     * <li>If the SDK finds an error for a key, the response will include a decision for the key showing <b>reasons</b> for the error.
     * <li>The SDK will always return key-mapped decisions. When it can not process requests, it’ll return an empty map after logging the errors.
     * </ul>
     * <p>
     * Note: This API is specifically designed for synchronous decision-making only.
     * For asynchronous decision-making, use the decideForKeysAsync() API.
     * </p>
     * @param keys A list of flag keys for which decisions will be made.
     * @param options A list of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    @Override
    public Map<String, OptimizelyDecision> decideForKeys(@NonNull List<String> keys,
                                                         @NonNull List<OptimizelyDecideOption> options) {
        return coreDecideForKeysSync(keys, options);
    }

    /**
     * Returns a key-map of decision results for multiple flag keys and a user context.
     *
     * <p>
     * Note: This API is specifically designed for synchronous decision-making only.
     * For asynchronous decision-making, use the decideForKeysAsync() API.
     * </p>
     * @param keys A list of flag keys for which decisions will be made.
     * @return All decision results mapped by flag keys.
     */
    @Override
    public Map<String, OptimizelyDecision> decideForKeys(@NonNull List<String> keys) {
        return coreDecideForKeysSync(keys, Collections.emptyList());
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     *
     * <p>
     * Note: This API is specifically designed for synchronous decision-making only.
     * For asynchronous decision-making, use the decideAllAsync() API.
     * </p>
     * @param options A list of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    @Override
    public Map<String, OptimizelyDecision> decideAll(@NonNull List<OptimizelyDecideOption> options) {
        return coreDecideAllSync(options);
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     *
     * <p>
     * Note: This API is specifically designed for synchronous decision-making only.
     * For asynchronous decision-making, use the decideAllAsync() API.
     * </p>
     * @return A dictionary of all decision results, mapped by flag keys.
     */
    @Override
    public Map<String, OptimizelyDecision> decideAll() {
        return coreDecideAllSync(Collections.emptyList());
    }

    // ===========================================
    // [ASYNCHRONOUS DECIDE METHODS WITH CALLBACKS]
    // ===========================================

    /**
     * Returns a decision result asynchronously for a given flag key and a user context.
     *
     * @param key A flag key for which a decision will be made.
     * @param callback A callback to invoke when the decision is available.
     * @param options A list of options for decision-making.
     */
    public void decideAsync(@NonNull String key,
                            @NonNull List<OptimizelyDecideOption> options,
                            @NonNull OptimizelyDecisionCallback callback) {
        coreDecideAsync(key, options, callback);
    }

    /**
     * Returns a decision result asynchronously for a given flag key and a user context.
     *
     * @param key A flag key for which a decision will be made.
     * @param callback A callback to invoke when the decision is available.
     */
    public void decideAsync(@NonNull String key, @NonNull OptimizelyDecisionCallback callback) {
        coreDecideAsync(key, Collections.emptyList(), callback);
    }

    /**
     * Returns decision results asynchronously for multiple flag keys.
     *
     * @param keys A list of flag keys for which decisions will be made.
     * @param callback A callback to invoke when decisions are available.
     * @param options A list of options for decision-making.
     */
    public void decideForKeysAsync(@NonNull List<String> keys,
                                   @NonNull List<OptimizelyDecideOption> options,
                                   @NonNull OptimizelyDecisionsCallback callback) {
        coreDecideForKeysAsync(keys, options, callback);
    }

    /**
     * Returns decision results asynchronously for multiple flag keys.
     *
     * @param keys A list of flag keys for which decisions will be made.
     * @param callback A callback to invoke when decisions are available.
     */
    public void decideForKeysAsync(@NonNull List<String> keys, @NonNull OptimizelyDecisionsCallback callback) {
        coreDecideForKeysAsync(keys, Collections.emptyList(), callback);
    }

    /**
     * Returns decision results asynchronously for all active flag keys.
     *
     * @param callback A callback to invoke when decisions are available.
     * @param options A list of options for decision-making.
     */
    public void decideAllAsync(@NonNull List<OptimizelyDecideOption> options,
                               @NonNull OptimizelyDecisionsCallback callback) {
        coreDecideAllAsync(options, callback);
    }

    /**
     * Returns decision results asynchronously for all active flag keys.
     *
     * @param callback A callback to invoke when decisions are available.
     */
    public void decideAllAsync(@NonNull OptimizelyDecisionsCallback callback) {
        coreDecideAllAsync(Collections.emptyList(), callback);
    }

    // ===========================================
    // [ASYNCHRONOUS DECIDE METHODS WITH BLOCKING CALLS]
    // - This will block the calling thread until a decision is returned.
    // - So this should be called in background thread only.
    // ===========================================

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     * <p>
     * Note: This method blocks the calling thread until the decision is complete.
     * It should only be called from a background thread.
     * For callback-based asynchronous decision-making, use decideAsync() methods.
     * </p>
     * @param key A flag key for which a decision will be made.
     * @param options A list of options for decision-making.
     * @return A decision result.
     */
    public OptimizelyDecision decideAsync(@NonNull String key,
                                                   @NonNull List<OptimizelyDecideOption> options) {
        return coreDecide(key, options);
    }

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     * <p>
     * Note: This method blocks the calling thread until the decision is complete.
     * It should only be called from a background thread.
     * </p>
     * @param key A flag key for which a decision will be made.
     * @return A decision result.
     */
    public OptimizelyDecision decideAsync(@NonNull String key) {
        return coreDecide(key, Collections.emptyList());
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for multiple flag keys and a user context.
     * <ul>
     * <li>If the SDK finds an error for a key, the response will include a decision for the key showing <b>reasons</b> for the error.
     * <li>The SDK will always return key-mapped decisions. When it can not process requests, it’ll return an empty map after logging the errors.
     * </ul>
     * @param keys A list of flag keys for which decisions will be made.
     * @param options A list of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideForKeysAsync(@NonNull List<String> keys,
                                                              @NonNull List<OptimizelyDecideOption> options) {
        return coreDecideForKeys(keys, options);
    }

    /**
     * Returns a key-map of decision results for multiple flag keys and a user context.
     *
     * @param keys A list of flag keys for which decisions will be made.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideForKeysAsync(@NonNull List<String> keys) {
        return coreDecideForKeys(keys, Collections.emptyList());
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     * <p>
     * Note: This method blocks the calling thread until decisions are complete.
     * It should only be called from a background thread.
     * </p>
     * @param options A list of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAllAsync(@NonNull List<OptimizelyDecideOption> options) {
        return coreDecideAll(options);
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     * <p>
     * Note: This method blocks the calling thread until decisions are complete.
     * It should only be called from a background thread.
     * </p>
     * @return A dictionary of all decision results, mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAllAsync() {
        return coreDecideAll(Collections.emptyList());
    }

    // ===========================================
    // Core Methods - All super calls centralized here for testability
    // ===========================================

    /**
     * Core delegation methods that encapsulate all java-sdk parent class method calls.
     * These methods enable clean unit testing by providing mockable entry points
     * for parent functionality, circumventing Mockito's inability to intercept super calls.
     */

    // [SYNCHRONOUS DECIDE METHODS]
    // - java-sdk provides these "decideSync" methods only for android-sdk we are using them directly.
    // - "decideSync" methods skip async decision like cmab for backward compatibility.

    OptimizelyDecision coreDecideSync(@NonNull String key, @NonNull List<OptimizelyDecideOption> options) {
        return super.decideSync(key, options);
    }

    Map<String, OptimizelyDecision> coreDecideForKeysSync(@NonNull List<String> keys, @NonNull List<OptimizelyDecideOption> options) {
        return super.decideForKeysSync(keys, options);
    }

    Map<String, OptimizelyDecision> coreDecideAllSync(@NonNull List<OptimizelyDecideOption> options) {
        return super.decideAllSync(options);
    }

    // [ASYNCHRONOUS DECIDE METHODS WITH CALLBACKS]
    // - java-sdk also provides async decide methods with callbacks, we are just delegating to super class here.

    void coreDecideAsync(@NonNull String key, @NonNull List<OptimizelyDecideOption> options, @NonNull OptimizelyDecisionCallback callback) {
        super.decideAsync(key, options, callback);
    }

    void coreDecideForKeysAsync(@NonNull List<String> keys, @NonNull List<OptimizelyDecideOption> options, @NonNull OptimizelyDecisionsCallback callback) {
        super.decideForKeysAsync(keys, options, callback);
    }

    void coreDecideAllAsync(@NonNull List<OptimizelyDecideOption> options, @NonNull OptimizelyDecisionsCallback callback) {
        super.decideAllAsync(options, callback);
    }

    // [ASYNCHRONOUS DECIDE METHODS WITH BLOCKING CALLS]
    // - these APIs call java-sdk native "decide" methods directly, blocking the calling thread until a decision is returned

    OptimizelyDecision coreDecide(@NonNull String key, @NonNull List<OptimizelyDecideOption> options) {
        return super.decide(key, options);
    }

    Map<String, OptimizelyDecision> coreDecideForKeys(@NonNull List<String> keys, @NonNull List<OptimizelyDecideOption> options) {
        return super.decideForKeys(keys, options);
    }

    Map<String, OptimizelyDecision> coreDecideAll(@NonNull List<OptimizelyDecideOption> options) {
        return super.decideAll(options);
    }

}
