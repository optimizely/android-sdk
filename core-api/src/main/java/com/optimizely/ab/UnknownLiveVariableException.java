/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab;

import com.optimizely.ab.config.LiveVariable;
import com.optimizely.ab.config.ProjectConfig;

/**
 * Exception thrown when attempting to use/refer to a {@link LiveVariable} that isn't present in the current
 * {@link ProjectConfig}.
 */
public class UnknownLiveVariableException extends OptimizelyRuntimeException {

    public UnknownLiveVariableException(String message) {
        super(message);
    }

    public UnknownLiveVariableException(String message, Throwable cause) {
        super(message, cause);
    }
}
