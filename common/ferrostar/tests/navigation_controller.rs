extern crate ferrostar;

use ferrostar::deviation_detection::RouteDeviationTracking;
use ferrostar::models::{Route, UserLocation};
use ferrostar::navigation_controller::models::{
    NavigationControllerConfig, StepAdvanceMode, TripState,
};
use ferrostar::navigation_controller::NavigationController;
use ferrostar::routing_adapters::osrm::OsrmResponseParser;
use ferrostar::routing_adapters::RouteResponseParser;
use std::time::SystemTime;

// A route with two steps
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
fn same_location_results_in_identical_state() {
    let route = get_route_with_two_steps();
    let initial_user_location = UserLocation {
        coordinates: route.steps[0].geometry[0],
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };

    let controller = NavigationController::new(
        route,
        NavigationControllerConfig {
            step_advance: StepAdvanceMode::Manual,
            route_deviation_tracking: RouteDeviationTracking::None,
        },
    );

    let initial_state = controller.get_initial_state(initial_user_location);
    assert!(matches!(initial_state, TripState::Navigating { .. }));

    // Nothing should happen if given the exact same user location update
    assert_eq!(
        controller.update_user_location(initial_user_location, &initial_state),
        initial_state
    );
}

#[test]
fn simple_route_state_machine_manual_advance() {
    let route = get_route_with_two_steps();
    let initial_user_location = UserLocation {
        coordinates: route.steps[0].geometry[0],
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };
    let user_location_end_of_first_step = UserLocation {
        coordinates: *route.steps[0].geometry.last().unwrap(),
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };

    let controller = NavigationController::new(
        route,
        NavigationControllerConfig {
            step_advance: StepAdvanceMode::Manual,
            route_deviation_tracking: RouteDeviationTracking::None,
        },
    );

    // The first update is meaningless in this test, except to get the state
    let initial_state = controller.get_initial_state(initial_user_location);
    let TripState::Navigating {
        snapped_user_location,
        remaining_steps: initial_remaining_steps,
        ..
    } = initial_state.clone()
    else {
        panic!("Expected state to be navigating");
    };
    // Assumption: our floating point epsilon doesn't create false positives here
    assert_eq!(initial_user_location, snapped_user_location);

    // The current step should not advance until we specifically trigger an advance
    let intermediate_state =
        controller.update_user_location(user_location_end_of_first_step, &initial_state);
    let TripState::Navigating {
        snapped_user_location,
        ..
    } = intermediate_state.clone()
    else {
        panic!("Expected state to be navigating");
    };

    assert_eq!(snapped_user_location, user_location_end_of_first_step);

    // Jump to the next step
    let terminal_state = controller.advance_to_next_step(&intermediate_state);
    let TripState::Navigating {
        remaining_steps, ..
    } = terminal_state.clone()
    else {
        panic!("Expected state to be navigating");
    };

    assert_ne!(initial_remaining_steps, remaining_steps);

    // There are only two steps, so advancing to the next step should put us in the "arrived" state
    assert!(matches!(
        controller.advance_to_next_step(&terminal_state),
        TripState::Complete { .. }
    ));
}

#[test]
fn simple_route_state_machine_advances_with_location_change() {
    let route = get_route_with_two_steps();
    let initial_user_location = UserLocation {
        coordinates: route.steps[0].geometry[0],
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };
    let user_location_end_of_first_step = UserLocation {
        coordinates: *route.steps[0].geometry.last().unwrap(),
        horizontal_accuracy: 0.0,
        course_over_ground: None,
        timestamp: SystemTime::now(),
    };

    let controller = NavigationController::new(
        route,
        NavigationControllerConfig {
            // NOTE: We will use an exact location to trigger the update;
            // this is not testing the thresholds.
            step_advance: StepAdvanceMode::DistanceToEndOfStep {
                distance: 0,
                minimum_horizontal_accuracy: 0,
            },
            route_deviation_tracking: RouteDeviationTracking::None,
        },
    );

    // The first update is meaningless in this test, except to get the state
    let initial_state = controller.get_initial_state(initial_user_location);
    let TripState::Navigating {
        remaining_steps: initial_remaining_steps,
        remaining_waypoints: initial_remaining_waypoints,
        ..
    } = initial_state.clone()
    else {
        panic!("Expected state to be navigating");
    };

    // The current step should change when we jump to the end location
    let TripState::Navigating {
        remaining_steps, remaining_waypoints, ..
    } = controller.update_user_location(user_location_end_of_first_step, &initial_state)
    else {
        panic!("Expected state to be navigating");
    };

    assert_ne!(remaining_steps, initial_remaining_steps);
    // In this case, the final step is the arrival point
    assert_eq!(remaining_waypoints.len(), 0);
}
