use std::sync::Arc;

use crate::{
    models::{Route, UserLocation},
    navigation_controller::{
        NavigationController, Navigator,
        models::{NavState, NavigationControllerConfig},
    },
};

#[cfg(test)]
pub(crate) mod test_helpers;

pub mod caching;
pub mod recording;
pub mod specialized;

#[cfg_attr(feature = "uniffi", uniffi::export(with_foreign))]
pub trait NavigationObserver: Send + Sync {
    fn on_get_initial_state(&self, state: NavState);
    fn on_user_location_update(&self, location: UserLocation, state: NavState);
    fn on_advance_to_next_step(&self, state: NavState);
    fn on_route_available(&self, route: Route);
}

#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct NavigationSession {
    pub controller: Arc<dyn Navigator>,
    pub observers: Vec<Arc<dyn NavigationObserver>>,
}

#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationSession {
    #[cfg_attr(feature = "uniffi", uniffi::constructor)]
    pub fn new(
        controller: Arc<dyn Navigator>,
        observers: Vec<Arc<dyn NavigationObserver>>,
    ) -> Self {
        Self {
            controller,
            observers,
        }
    }
}

#[cfg_attr(feature = "uniffi", uniffi::export)]
impl Navigator for NavigationSession {
    fn route(&self) -> Route {
        self.controller.route()
    }

    fn get_initial_state(&self, location: UserLocation) -> NavState {
        let state = self.controller.get_initial_state(location);
        for observer in &self.observers {
            observer.on_route_available(self.route());
            observer.on_get_initial_state(state.clone());
        }
        state
    }

    fn update_user_location(&self, location: UserLocation, state: NavState) -> NavState {
        let state = self.controller.update_user_location(location, state);
        for observer in &self.observers {
            observer.on_user_location_update(location, state.clone());
        }
        state
    }

    fn advance_to_next_step(&self, state: NavState) -> NavState {
        let state = self.controller.advance_to_next_step(state);
        for observer in &self.observers {
            observer.on_advance_to_next_step(state.clone());
        }
        state
    }
}

/// Creates a new navigation session for the given route and configuration.
///
/// It returns an Arc-wrapped trait object implementing `Navigator`.
/// If `should_record` is true, it creates a controller with event recording enabled.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub fn create_navigation_session(
    route: Route,
    config: NavigationControllerConfig,
    observers: Vec<Arc<dyn NavigationObserver>>,
) -> NavigationSession {
    // Creates a navigation session with a controller and observers.
    NavigationSession::new(
        Arc::new(NavigationController::new(route, config)),
        observers,
    )
}
