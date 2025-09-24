use crate::{deviation_detection::RouteDeviation, models::Route, navigation_controller::{models::{NavState, TripState}, Navigator}, navigation_session::NavigationSession, simulation::{advance_location_simulation, location_simulation_from_route, LocationBias}};

pub(crate) fn test_full_route_state_snapshot(
    route: Route,
    session: NavigationSession,
) -> Vec<NavState> {
    let mut simulation_state =
        location_simulation_from_route(&route, Some(10.0), LocationBias::None)
            .expect("Unable to create simulation");

    let mut state = session.get_initial_state(simulation_state.current_location);
    let mut states = vec![state.clone()];
    loop {
        let new_simulation_state = advance_location_simulation(&simulation_state);
        let new_state =
            session.update_user_location(new_simulation_state.current_location, state);

        match new_state.trip_state() {
            TripState::Idle { .. } => {}
            TripState::Navigating {
                current_step_geometry_index,
                ref remaining_steps,
                ref deviation,
                ..
            } => {
                if let Some(index) = current_step_geometry_index {
                    let geom_length = remaining_steps[0].geometry.len() as u64;
                    // Regression test that the geometry index is valid
                    assert!(
                        index < geom_length,
                        "index = {index}, geom_length = {geom_length}"
                    );
                }

                // Regression test that we are never marked as off the route.
                // We used to encounter this with relative step advance on self-intersecting
                // routes, for example.
                assert_eq!(deviation, &RouteDeviation::NoDeviation);
            }
            TripState::Complete { .. } => {
                states.push(new_state);
                break;
            }
        }

        simulation_state = new_simulation_state;
        state = new_state.clone();
        states.push(new_state);
    }

    states
}
