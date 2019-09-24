package com.optimizely.ab.fsc_app.bdd.support;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;

import org.yaml.snakeyaml.Yaml;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private final static String EXP_ID = "\\{\\{#expId\\}\\}(\\S+)*\\{\\{/expId\\}\\}";
    private final static String DATAFILE_PROJECT_ID = "\\{\\{datafile.projectId\\}\\}";
    private final static String EXP_CAMPAIGN_ID = "\\{\\{#expCampaignId\\}\\}(\\S+)*\\{\\{/expCampaignId\\}\\}";
    private final static String VAR_ID = "\\{\\{#varId\\}\\}(\\S+)*\\{\\{/varId\\}\\}";

    public static Object parseYAML(String args, ProjectConfig projectConfig) {
        if ("NULL".equals(args) || args.isEmpty()) {
            return null;
        }
        args = findAndReplaceAllMustacheRegex(args, projectConfig);
        Yaml yaml = new Yaml();
        return yaml.load(args);
    }

    private static String findAndReplaceAllMustacheRegex(String yaml, ProjectConfig projectConfig) {
        Pattern expIdPattern = Pattern.compile(EXP_ID);
        Matcher expIdMatcher = expIdPattern.matcher(yaml);
        while (expIdMatcher.find()) {
            Experiment experiment = Objects.requireNonNull(projectConfig).getExperimentForKey(expIdMatcher.group(1), null);
            if (experiment != null) {
                yaml = yaml.replace(expIdMatcher.group(0), experiment.getId());
            }
        }

        Pattern campaignIdPattern = Pattern.compile(EXP_CAMPAIGN_ID);
        Matcher campaignIdMatcher = campaignIdPattern.matcher(yaml);
        while (campaignIdMatcher.find()) {
            Experiment experiment = projectConfig.getExperimentForKey(campaignIdMatcher.group(1), null);
            if (experiment != null) {
                yaml = yaml.replace(campaignIdMatcher.group(0), experiment.getLayerId());
            }
        }

        Pattern varIdPattern = Pattern.compile(VAR_ID);
        Matcher varIdMatcher = varIdPattern.matcher(yaml);
        while (varIdMatcher.find()) {
            String[] expVarKey = varIdMatcher.group(1).split("\\.");
            Experiment experiment = projectConfig.getExperimentForKey(expVarKey[0], null);
            if (experiment != null) {
                Variation variation = experiment.getVariationKeyToVariationMap().get(expVarKey[1]);
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

// TODO: Will use these methods for deep merging to hashmaps for comparision of missing keys
//
//    public static Map deepMerge(Map original, Map newMap) {
//        for (Object key : newMap.keySet()) {
//            if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
//                Map originalChild = (Map) original.get(key);
//                Map newChild = (Map) newMap.get(key);
//                original.put(key, deepMerge(originalChild, newChild));
//            } else {
//                original.put(key, newMap.get(key));
//            }
//        }
//        return original;
//    }
//
//    public static void mergeWithSideEffect(final Map<String, Object> target, final Map<String, Object> source) {
//        source.forEach((key, sourceObj) -> {
//            Object targetObj = target.get(key);
//            if (sourceObj instanceof Map && targetObj instanceof Map) {
//                mergeWithSideEffect((Map) targetObj, (Map) sourceObj);
//            } else if (sourceObj instanceof List && targetObj instanceof List) {
//                final List<Object> temp = new ArrayList<Object>((List) targetObj);
//                mergeWithSideEffect(temp, (List) sourceObj);
//                targetObj = temp;
//            } else if (sourceObj instanceof Set && targetObj instanceof Set) {
//                final Set<Object> temp = new HashSet<>((Set) targetObj);
//                mergeWithSideEffect(temp, (Set) sourceObj);
//                targetObj = temp;
//            }
//            target.put(key, sourceObj);
//        });
//    }
//
//    private static void mergeWithSideEffect(final Collection target, final Collection source) {
//        source.stream()
//                .forEach(target::add);
//    }
}
