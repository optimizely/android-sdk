@ALL
@FEATURE_EXPERIMENT
Feature: IsFeatureEnabled API - Audience Targeting

  Background:
    Given the datafile is "feature_exp.json"
    And 1 "Activate" listener is added

  Scenario: Targeted feature experiment with no attribute match
    When is_feature_enabled is called with arguments
    """
      feature_flag_key: feature_exp_running_targeted
      user_id: test_user_1
      attributes:
        s_foo: not_foo
    """
    Then the result should be "false"
    And in the response, "listener_called" should be "NULL"
    And there are no dispatched events

  Scenario: Targeted feature experiment with matching attribute
    When is_feature_enabled is called with arguments
    """
      feature_flag_key: feature_exp_running_targeted
      user_id: test_user_1
      attributes:
        s_foo: foo
    """
    Then the result should be "true"
    And in the response, "listener_called" should match
    """
      - experiment_key: feature_exp_running_targeted
        user_id: test_user_1
        variation_key: all_traffic_variation
        attributes:
          s_foo: foo
    """
    And dispatched events payloads include
    """
      - project_id: "{{datafile.projectId}}"
        visitors:
        - visitor_id: test_user_1
          attributes:
          - key: s_foo
            type: custom
            value: foo
          snapshots:
            - decisions:
              - experiment_id: "{{#expId}}feature_exp_running_targeted{{/expId}}"
                variation_id: "{{#varId}}feature_exp_running_targeted.all_traffic_variation{{/varId}}"
                campaign_id: "{{#expCampaignId}}feature_exp_running_targeted{{/expCampaignId}}"
            - events:
              - key: campaign_activated
                entity_id: "{{#expCampaignId}}feature_exp_running_targeted{{/expCampaignId}}"
    """
