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
    public OptimizelyUserContextAndroid(@NonNull Optimizely optimizely,
                                        @NonNull String userId,
                                        @NonNull Map<String, ?> attributes) {
        super(optimizely, userId, attributes);
    }

    public OptimizelyUserContextAndroid(@NonNull Optimizely optimizely,
                                        @NonNull String userId,
                                        @NonNull Map<String, ?> attributes,
                                        @Nullable Map<String, OptimizelyForcedDecision> forcedDecisionsMap,
                                        @Nullable List<String> qualifiedSegments) {
        super(optimizely, userId, attributes, forcedDecisionsMap, qualifiedSegments);
    }

    public OptimizelyUserContextAndroid(@NonNull Optimizely optimizely,
                                 @NonNull String userId,
                                 @NonNull Map<String, ?> attributes,
                                 @Nullable Map<String, OptimizelyForcedDecision> forcedDecisionsMap,
                                 @Nullable List<String> qualifiedSegments,
                                 @Nullable Boolean shouldIdentifyUser) {
        super(optimizely, userId, attributes, forcedDecisionsMap, qualifiedSegments, shouldIdentifyUser);
    }

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     * <ul>
     * <li>If the SDK finds an error, it’ll return a decision with <b>null</b> for <b>variationKey</b>. The decision will include an error message in <b>reasons</b>.
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
        return decideSync(key, options);
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
        return decide(key, Collections.emptyList());
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
        return decideForKeysSync(keys, options);
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
        return decideForKeys(keys, Collections.emptyList());
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
        return decideAllSync(options);
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
        return decideAll(Collections.emptyList());
    }

    // ===========================================
    // Async Methods (Android-specific) with callbacks
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
        super.decideAsync(key, options, callback);
    }

    /**
     * Returns a decision result asynchronously for a given flag key and a user context.
     *
     * @param key A flag key for which a decision will be made.
     * @param callback A callback to invoke when the decision is available.
     */
    public void decideAsync(@NonNull String key, @NonNull OptimizelyDecisionCallback callback) {
        decideAsync(key, Collections.emptyList(), callback);
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
        super.decideForKeysAsync(keys, options, callback);
    }

    /**
     * Returns decision results asynchronously for multiple flag keys.
     *
     * @param keys A list of flag keys for which decisions will be made.
     * @param callback A callback to invoke when decisions are available.
     */
    public void decideForKeysAsync(@NonNull List<String> keys, @NonNull OptimizelyDecisionsCallback callback) {
        decideForKeysAsync(keys, Collections.emptyList(), callback);
    }

    /**
     * Returns decision results asynchronously for all active flag keys.
     *
     * @param callback A callback to invoke when decisions are available.
     * @param options A list of options for decision-making.
     */
    public void decideAllAsync(@NonNull List<OptimizelyDecideOption> options,
                               @NonNull OptimizelyDecisionsCallback callback) {
        super.decideAllAsync(options, callback);
    }

    /**
     * Returns decision results asynchronously for all active flag keys.
     *
     * @param callback A callback to invoke when decisions are available.
     */
    public void decideAllAsync(@NonNull OptimizelyDecisionsCallback callback) {
        decideAllAsync(Collections.emptyList(), callback);
    }

    // ===========================================
    // Async Methods (Android-specific) with blocking calls to synchronous methods
    // ===========================================

    public OptimizelyDecision decideAsync(@Nonnull String key,
                                     @Nonnull List<OptimizelyDecideOption> options) {
        return super.decide(key, options);
    }

    /**
     * Returns a decision result ({@link OptimizelyDecision}) for a given flag key and a user context, which contains all data required to deliver the flag.
     *
     * @param key A flag key for which a decision will be made.
     * @return A decision result.
     */
    public OptimizelyDecision decideAsync(@Nonnull String key) {
        return decideAsync(key, Collections.emptyList());
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
    public Map<String, OptimizelyDecision> decideForKeysAsync(@Nonnull List<String> keys,
                                                         @Nonnull List<OptimizelyDecideOption> options) {
        return super.decideForKeys(keys, options);
    }

    /**
     * Returns a key-map of decision results for multiple flag keys and a user context.
     *
     * @param keys A list of flag keys for which decisions will be made.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideForKeysAsync(@Nonnull List<String> keys) {
        return decideForKeysAsync(keys, Collections.emptyList());
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     *
     * @param options A list of options for decision-making.
     * @return All decision results mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAllAsync(@Nonnull List<OptimizelyDecideOption> options) {
        return super.decideAll(options);
    }

    /**
     * Returns a key-map of decision results ({@link OptimizelyDecision}) for all active flag keys.
     *
     * @return A dictionary of all decision results, mapped by flag keys.
     */
    public Map<String, OptimizelyDecision> decideAllAsync() {
        return decideAllAsync(Collections.emptyList());
    }

    // ===========================================
    // Override methods for testability
    // These methods enable Mockito spies to intercept calls that would
    // otherwise go directly to super.method() and be unverifiable
    // ===========================================

    public OptimizelyDecision decideSync(@NonNull String key, @NonNull List<OptimizelyDecideOption> options) {
        return super.decideSync(key, options);
    }

    public Map<String, OptimizelyDecision> decideForKeysSync(@NonNull List<String> keys, @NonNull List<OptimizelyDecideOption> options) {
        return super.decideForKeysSync(keys, options);
    }

    public Map<String, OptimizelyDecision> decideAllSync(@NonNull List<OptimizelyDecideOption> options) {
        return super.decideAllSync(options);
    }

}
