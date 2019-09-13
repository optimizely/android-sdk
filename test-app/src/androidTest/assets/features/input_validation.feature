Feature: IsFeatureEnabled API - Input Validation

  Background:
    Given the datafile is "feature_exp.json"
    And 1 "Activate" listener is added

  Scenario Outline: feature_flag_key invalid cases
    When is_feature_enabled is called with arguments
    """
    feature_flag_key: <feature_flag_key>
    user_id: test_user
    """
    Then the result should be "false"
    And in the response, "listener_called" should be "NULL"
    And there are no dispatched events

    Examples:
      | feature_flag_key |
      | null             |
      | ""               |
      | not_in_datafile  |
