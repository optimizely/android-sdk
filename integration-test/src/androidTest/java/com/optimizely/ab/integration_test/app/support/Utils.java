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

package com.optimizely.ab.integration_test.app.support;

import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;

import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.optimizely.ab.integration_test.app.support.OptlyDataHelper.getAttributeByKey;
import static com.optimizely.ab.integration_test.app.support.OptlyDataHelper.getEventByKey;
import static com.optimizely.ab.integration_test.app.support.OptlyDataHelper.getExperimentByKey;
import static com.optimizely.ab.integration_test.app.support.OptlyDataHelper.getVariationByKey;
import static com.optimizely.ab.integration_test.app.support.OptlyDataHelper.projectConfig;


class Utils {
    private final static String EXP_ID = "\\{\\{#expId\\}\\}(\\S+)*\\{\\{/expId\\}\\}";
    private final static String EVENT_ID = "\\{\\{#eventId\\}\\}(\\S+)*\\{\\{/eventId\\}\\}";
    private final static String DATAFILE_PROJECT_ID = "\\{\\{datafile.projectId\\}\\}";
    private final static String EXP_CAMPAIGN_ID = "\\{\\{#expCampaignId\\}\\}(\\S+)*\\{\\{/expCampaignId\\}\\}";
    private final static String VAR_ID = "\\{\\{#varId\\}\\}(\\S+)*\\{\\{/varId\\}\\}";
    private final static String ATTRIBUTE_ID = "\\{\\{#attributeId\\}\\}(\\S+)*\\{\\{/attributeId\\}\\}";

    static Object parseYAML(String args) {
        if (args == null || "NULL".equals(args) || args.isEmpty()) {
            return null;
        }
        args = findAndReplaceAllMustacheRegex(args);
        Yaml yaml = new Yaml();
        return yaml.load(args);
    }

    private static String findAndReplaceAllMustacheRegex(String yaml) {
        if (projectConfig == null)
            return null;

        Pattern expIdPattern = Pattern.compile(EXP_ID);
        Matcher expIdMatcher = expIdPattern.matcher(yaml);
        while (expIdMatcher.find()) {
            Experiment experiment = getExperimentByKey(expIdMatcher.group(1));
            if (experiment != null) {
                yaml = yaml.replace(expIdMatcher.group(0), experiment.getId());
            }
        }

        Pattern campaignIdPattern = Pattern.compile(EXP_CAMPAIGN_ID);
        Matcher campaignIdMatcher = campaignIdPattern.matcher(yaml);
        while (campaignIdMatcher.find()) {
            Experiment experiment = getExperimentByKey(campaignIdMatcher.group(1));
            if (experiment != null) {
                yaml = yaml.replace(campaignIdMatcher.group(0), experiment.getLayerId());
            }
        }

        Pattern eventIdPattern = Pattern.compile(EVENT_ID);
        Matcher eventIdMatcher = eventIdPattern.matcher(yaml);
        while (eventIdMatcher.find()) {
            EventType eventType = getEventByKey(eventIdMatcher.group(1));
            if (eventType != null) {
                yaml = yaml.replace(eventIdMatcher.group(0), eventType.getId());
            }
        }

        Pattern attrIdPattern = Pattern.compile(ATTRIBUTE_ID);
        Matcher attrIdMatcher = attrIdPattern.matcher(yaml);
        while (attrIdMatcher.find()) {
            String attributeId = getAttributeByKey(attrIdMatcher.group(1));
            if (attributeId != null) {
                yaml = yaml.replace(attrIdMatcher.group(0), attributeId);
            }
        }

        Pattern varIdPattern = Pattern.compile(VAR_ID);
        Matcher varIdMatcher = varIdPattern.matcher(yaml);
        while (varIdMatcher.find()) {
            String[] expVarKey = varIdMatcher.group(1).split("\\.");
            Experiment experiment = getExperimentByKey(expVarKey[0]);
            if (experiment != null) {
                Variation variation = getVariationByKey(experiment, expVarKey[1]);
                if (variation != null)
                    yaml = yaml.replace(varIdMatcher.group(0), variation.getId());
            }
        }

        Pattern datafilePattern = Pattern.compile(DATAFILE_PROJECT_ID);
        Matcher datafileMatcher = datafilePattern.matcher(yaml);
        if (datafileMatcher.find()) {
            yaml = datafileMatcher.replaceAll(projectConfig.getProjectId());
        }

        return yaml;
    }

    /**
     * @param subset Object which you want to make sure that actual Object contains all its keys and values
     * @param actual Object which should contain all subset key value pairs.
     * @return True if all key value pairs of subset map exist and matches in actual object else return False.
     */
    static Boolean containsSubset(Map<String, Object> subset, Map<String, Object> actual) {
        if (subset == null)
            return subset == actual;

        AtomicReference<Boolean> result = new AtomicReference<>(true);
        for (Map.Entry<String, Object> entry : subset.entrySet()) {
            String key = entry.getKey();
            Object sourceObj = entry.getValue();

            if (!actual.containsKey(key)) {
                result.set(false);
                break;
            }
            Object targetObj = actual.get(key);
            if (sourceObj instanceof Map && targetObj instanceof Map) {
                if (!containsSubset((Map) sourceObj, (Map) targetObj)) {
                    result.set(false);
                    break;
                }
            } else if (sourceObj instanceof List && targetObj instanceof List) {
                final List<Object> temp = new ArrayList<Object>((List) targetObj);
                if (!containsSubset((List) sourceObj, temp)) {
                    result.set(false);
                    break;
                }
            } else if (sourceObj instanceof String) {
                if (!sourceObj.equals(targetObj)) {
                    result.set(false);
                    break;
                }
            } else if (sourceObj == null && targetObj == null) {
            } else if (!sourceObj.equals(targetObj)) {
                result.set(false);
                break;
            }

        }
        return result.get();
    }

    static Boolean containsSubset(final List subset, final List actual) {
        if (subset.size() > actual.size()) {
            return false;
        }
        for (int i = 0; i < subset.size(); i++) {
            boolean found = false;
            for (int j = 0; j < actual.size(); j++) {
                if (containsSubset((Map) subset.get(i), (Map) actual.get(j))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    static Object copyResponse(int count, Object args) {
        if (args instanceof List) {
            List argsObject = (List) args;
            List cloneArgs = new ArrayList<>();
            if (argsObject.size() > 0) {
                for (int i = 0; i < count; i++) {
                    cloneArgs.addAll(argsObject);
                }
                return cloneArgs;
            }
        }
        return args;
    }
}
