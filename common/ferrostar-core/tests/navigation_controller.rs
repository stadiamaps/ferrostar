extern crate ferrostar_core;

use ferrostar_core::routing_adapters::osrm::OsrmResponseParser;
use ferrostar_core::{
    NavigationController, NavigationControllerConfig, NavigationStateUpdate, Route,
    RouteResponseParser, StepAdvanceMode, UserLocation,
};
use std::time::SystemTime;

const TWO_STEP_RESPONSE: &str = r#"{"routes":[{"weight_name":"auto","weight":56.002,"duration":11.488,"distance":284,"legs":[{"via_waypoints":[],"annotation":{"maxspeed":[{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"}],"speed":[24.7,24.7,24.7,24.7,24.7,24.7,24.7,24.7,24.7],"distance":[23.6,14.9,9.6,13.2,25,28.1,38.1,41.6,90],"duration":[0.956,0.603,0.387,0.535,1.011,1.135,1.539,1.683,3.641]},"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":56.002,"duration":11.488,"steps":[{"intersections":[{"bearings":[288],"entry":[true],"admin_index":0,"out":0,"geometry_index":0,"location":[-149.543469,60.534716]}],"speedLimitUnit":"mph","maneuver":{"type":"depart","instruction":"Drive west on AK 1/Seward Highway.","bearing_after":288,"bearing_before":0,"location":[-149.543469,60.534716]},"speedLimitSign":"mutcd","name":"Seward Highway","duration":11.488,"distance":284,"driving_side":"right","weight":56.002,"mode":"driving","ref":"AK 1","geometry":"wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB"},{"intersections":[{"bearings":[89],"entry":[true],"in":0,"admin_index":0,"geometry_index":9,"location":[-149.548581,60.534991]}],"speedLimitUnit":"mph","maneuver":{"type":"arrive","instruction":"You have arrived at your destination.","bearing_after":0,"bearing_before":269,"location":[-149.548581,60.534991]},"speedLimitSign":"mutcd","name":"Seward Highway","duration":0,"distance":0,"driving_side":"right","weight":0,"mode":"driving","ref":"AK 1","geometry":"}kwmrBhavf|G??"}],"distance":284,"summary":"AK 1"}],"geometry":"wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB"}],"waypoints":[{"distance":0,"name":"AK 1","location":[-149.543469,60.534715]},{"distance":0,"name":"AK 1","location":[-149.548581,60.534991]}],"code":"Ok"}"#;

/// Gets a route with two steps.
///
/// The accuracy of each parser is tested separately in the routing_adapters module;
/// this function simply intends to return a route with two steps.
fn get_route_with_two_steps() -> Route {
    let parser = OsrmResponseParser::new(6);
    parser
        .parse_response(TWO_STEP_RESPONSE.into())
        .expect("Unable to parse OSRM response")
        .pop()
        .expect("Expected a route")
}

#[test]
fn simple_route_state_machine_manual_advance() {
    let route = get_route_with_two_steps();
    let initial_user_location = UserLocation {
        coordinates: route.steps[0].start_location,
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };
    let user_location_end_of_first_step = UserLocation {
        coordinates: route.steps[0].end_location,
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };

    let controller = NavigationController::new(
        initial_user_location,
        route,
        NavigationControllerConfig {
            step_advance: StepAdvanceMode::Manual,
        },
    );

    // The first update is meaningless in this test, except to get the state
    let initial_state = controller.update_user_location(initial_user_location);
    let NavigationStateUpdate::Navigating {
        snapped_user_location,
        current_step: first_step,
        ..
    } = controller.update_user_location(initial_user_location)
    else {
        panic!("Expected state to be navigating");
    };
    assert_eq!(initial_user_location, snapped_user_location);

    // Nothing should happen if given the exact same user location update
    assert_eq!(
        controller.update_user_location(initial_user_location),
        initial_state
    );

    // The current step should not advance until we specifically trigger an advance
    assert!(matches!(
        controller.update_user_location(user_location_end_of_first_step),
        NavigationStateUpdate::Navigating {
            current_step: first_step,
            ..
        }
    ));

    // Jump to the next step
    let NavigationStateUpdate::Navigating {
        current_step: second_step,
        ..
    } = controller.advance_to_next_step()
    else {
        panic!("Expected state to be navigating");
    };

    assert_ne!(first_step, second_step);

    // There are only two steps, so advancing to the next step should put us in the "arrived" state
    assert!(matches!(
        controller.advance_to_next_step(),
        NavigationStateUpdate::Arrived { .. }
    ));
}

#[test]
fn simple_route_state_machine_advances_with_location_change() {
    let route = get_route_with_two_steps();
    let initial_user_location = UserLocation {
        coordinates: route.steps[0].start_location,
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };
    let user_location_end_of_first_step = UserLocation {
        coordinates: route.steps[0].end_location,
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };

    let controller = NavigationController::new(
        initial_user_location,
        route,
        NavigationControllerConfig {
            // NOTE: We will use an exact location to trigger the update;
            // this is not testing the thresholds.
            step_advance: StepAdvanceMode::DistanceToLastWaypoint { distance: 0, minimum_horizontal_accuracy: 0 },
        },
    );

    // The first update is meaningless in this test, except to get the state
    let initial_state = controller.update_user_location(initial_user_location);
    let NavigationStateUpdate::Navigating {
        current_step: first_step,
        ..
    } = controller.update_user_location(initial_user_location)
    else {
        panic!("Expected state to be navigating");
    };

    // Nothing should happen if given the exact same user location update
    assert_eq!(
        controller.update_user_location(initial_user_location),
        initial_state
    );

    // The current step should change when we jump to the end location
    let NavigationStateUpdate::Navigating {
        current_step: second_step,
        ..
    } = controller.update_user_location(user_location_end_of_first_step)
    else {
        panic!("Expected state to be navigating");
    };

    assert_ne!(first_step, second_step);
}

