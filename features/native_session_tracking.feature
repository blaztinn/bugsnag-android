Feature: NDK Session Tracking

Scenario: Stopped session is not in payload of unhandled NDK error
    And I run "CXXStopSessionScenario" and relaunch the app
    And I configure Bugsnag for "CXXStopSessionScenario"
    And I wait to receive 2 requests
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session" is null

Scenario: Started session is in payload of unhandled NDK error
    And I run "CXXStartSessionScenario" and relaunch the app
    And I configure Bugsnag for "CXXStartSessionScenario"
    And I wait to receive 2 requests
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
   And the payload field "events.0.session.events.unhandled" equals 1
