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
package com.optimizely.ab.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.audience.Condition;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the Optimizely Project configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectConfig {

    public enum Version {
        V2 ("2"),
        V3 ("3"),
        V4 ("4");

        private final String version;

        Version(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return version;
        }
    }

    private final String accountId;
    private final String projectId;
    private final String revision;
    private final String version;
    private final boolean anonymizeIP;
    private final List<Attribute> attributes;
    private final List<Audience> audiences;
    private final List<EventType> events;
    private final List<Experiment> experiments;
    private final List<FeatureFlag> featureFlags;
    private final List<Group> groups;
    private final List<LiveVariable> liveVariables;

    // key to entity mappings
    private final Map<String, Attribute> attributeKeyMapping;
    private final Map<String, EventType> eventNameMapping;
    private final Map<String, Experiment> experimentKeyMapping;
    private final Map<String, FeatureFlag> featureKeyMapping;
    private final Map<String, LiveVariable> liveVariableKeyMapping;

    // id to entity mappings
    private final Map<String, Audience> audienceIdMapping;
    private final Map<String, Experiment> experimentIdMapping;
    private final Map<String, Group> groupIdMapping;

    // other mappings
    private final Map<String, List<Experiment>> liveVariableIdToExperimentsMapping;
    private final Map<String, Map<String, LiveVariableUsageInstance>> variationToLiveVariableUsageInstanceMapping;

    // v2 constructor
    public ProjectConfig(String accountId, String projectId, String version, String revision, List<Group> groups,
                         List<Experiment> experiments, List<Attribute> attributes, List<EventType> eventType,
                         List<Audience> audiences) {
        this(accountId, projectId, version, revision, groups, experiments, attributes, eventType, audiences, false,
             null);
    }

    // v3 constructor
    public ProjectConfig(String accountId, String projectId, String version, String revision, List<Group> groups,
                         List<Experiment> experiments, List<Attribute> attributes, List<EventType> eventType,
                         List<Audience> audiences, boolean anonymizeIP, List<LiveVariable> liveVariables) {
        this(
                accountId,
                anonymizeIP,
                projectId,
                revision,
                version,
                attributes,
                audiences,
                eventType,
                experiments,
                null,
                groups,
                liveVariables
        );
    }

    // v4 constructor
    public ProjectConfig(String accountId,
                         boolean anonymizeIP,
                         String projectId,
                         String revision,
                         String version,
                         List<Attribute> attributes,
                         List<Audience> audiences,
                         List<EventType> events,
                         List<Experiment> experiments,
                         List<FeatureFlag> featureFlags,
                         List<Group> groups,
                         List<LiveVariable> liveVariables) {

        this.accountId = accountId;
        this.projectId = projectId;
        this.version = version;
        this.revision = revision;
        this.anonymizeIP = anonymizeIP;

        this.attributes = Collections.unmodifiableList(attributes);
        this.audiences = Collections.unmodifiableList(audiences);
        this.events = Collections.unmodifiableList(events);
        if (featureFlags == null) {
            this.featureFlags = Collections.emptyList();
        }
        else {
            this.featureFlags = Collections.unmodifiableList(featureFlags);
        }

        this.groups = Collections.unmodifiableList(groups);

        List<Experiment> allExperiments = new ArrayList<Experiment>();
        allExperiments.addAll(experiments);
        allExperiments.addAll(aggregateGroupExperiments(groups));
        this.experiments = Collections.unmodifiableList(allExperiments);

        // generate the name mappers
        this.attributeKeyMapping = ProjectConfigUtils.generateNameMapping(attributes);
        this.eventNameMapping = ProjectConfigUtils.generateNameMapping(this.events);
        this.experimentKeyMapping = ProjectConfigUtils.generateNameMapping(this.experiments);
        this.featureKeyMapping = ProjectConfigUtils.generateNameMapping(this.featureFlags);

        // generate audience id to audience mapping
        this.audienceIdMapping = ProjectConfigUtils.generateIdMapping(audiences);
        this.experimentIdMapping = ProjectConfigUtils.generateIdMapping(this.experiments);
        this.groupIdMapping = ProjectConfigUtils.generateIdMapping(groups);

        if (liveVariables == null) {
            this.liveVariables = null;
            this.liveVariableKeyMapping = Collections.emptyMap();
            this.liveVariableIdToExperimentsMapping = Collections.emptyMap();
            this.variationToLiveVariableUsageInstanceMapping = Collections.emptyMap();
        } else {
            this.liveVariables = Collections.unmodifiableList(liveVariables);
            this.liveVariableKeyMapping = ProjectConfigUtils.generateNameMapping(this.liveVariables);
            this.liveVariableIdToExperimentsMapping =
                    ProjectConfigUtils.generateLiveVariableIdToExperimentsMapping(this.experiments);
            this.variationToLiveVariableUsageInstanceMapping =
                    ProjectConfigUtils.generateVariationToLiveVariableUsageInstancesMap(this.experiments);
        }
    }

    private List<Experiment> aggregateGroupExperiments(List<Group> groups) {
        List<Experiment> groupExperiments = new ArrayList<Experiment>();
        for (Group group : groups) {
            groupExperiments.addAll(group.getExperiments());
        }

        return groupExperiments;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getVersion() {
        return version;
    }

    public String getRevision() {
        return revision;
    }

    public boolean getAnonymizeIP() {
        return anonymizeIP;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public List<Experiment> getExperimentsForEventKey(String eventKey) {
        EventType event = eventNameMapping.get(eventKey);
        if (event != null) {
            List<String> experimentIds = event.getExperimentIds();
            List<Experiment> experiments = new ArrayList<Experiment>(experimentIds.size());
            for (String experimentId : experimentIds) {
                experiments.add(experimentIdMapping.get(experimentId));
            }

            return experiments;
        }

        return Collections.emptyList();
    }

    public List<FeatureFlag> getFeatureFlags() {
        return featureFlags;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<EventType> getEventTypes() {
        return events;
    }

    public List<Audience> getAudiences() {
        return audiences;
    }

    public Condition getAudienceConditionsFromId(String audienceId) {
        Audience audience = audienceIdMapping.get(audienceId);

        return audience != null ? audience.getConditions() : null;
    }

    public List<LiveVariable> getLiveVariables() {
        return liveVariables;
    }

    public Map<String, Experiment> getExperimentKeyMapping() {
        return experimentKeyMapping;
    }

    public Map<String, Attribute> getAttributeKeyMapping() {
        return attributeKeyMapping;
    }

    public Map<String, EventType> getEventNameMapping() {
        return eventNameMapping;
    }

    public Map<String, Audience> getAudienceIdMapping() {
        return audienceIdMapping;
    }

    public Map<String, Experiment> getExperimentIdMapping() {
        return experimentIdMapping;
    }

    public Map<String, Group> getGroupIdMapping() {
        return groupIdMapping;
    }

    public Map<String, LiveVariable> getLiveVariableKeyMapping() {
        return liveVariableKeyMapping;
    }

    public Map<String, List<Experiment>> getLiveVariableIdToExperimentsMapping() {
        return liveVariableIdToExperimentsMapping;
    }

    public Map<String, Map<String, LiveVariableUsageInstance>> getVariationToLiveVariableUsageInstanceMapping() {
        return variationToLiveVariableUsageInstanceMapping;
    }

    public Map<String, FeatureFlag> getFeatureKeyMapping() {
        return featureKeyMapping;
    }

    @Override
    public String toString() {
        return "ProjectConfig{" +
                "accountId='" + accountId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", revision='" + revision + '\'' +
                ", version='" + version + '\'' +
                ", anonymizeIP='" + anonymizeIP + '\'' +
                ", groups=" + groups +
                ", experiments=" + experiments +
                ", attributes=" + attributes +
                ", events=" + events +
                ", audiences=" + audiences +
                ", liveVariables=" + liveVariables +
                ", experimentKeyMapping=" + experimentKeyMapping +
                ", attributeKeyMapping=" + attributeKeyMapping +
                ", liveVariableKeyMapping=" + liveVariableKeyMapping +
                ", eventNameMapping=" + eventNameMapping +
                ", audienceIdMapping=" + audienceIdMapping +
                ", experimentIdMapping=" + experimentIdMapping +
                ", groupIdMapping=" + groupIdMapping +
                ", liveVariableIdToExperimentsMapping=" + liveVariableIdToExperimentsMapping +
                ", variationToLiveVariableUsageInstanceMapping=" + variationToLiveVariableUsageInstanceMapping +
                '}';
    }
}
