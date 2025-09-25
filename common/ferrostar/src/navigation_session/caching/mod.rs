use crate::{models::UserLocation, navigation_controller::models::NavState, navigation_session::NavigationObserver};

pub mod models;

struct NavigationSessionCaching {

}

impl NavigationObserver for NavigationSessionCaching {
    fn on_get_initial_state(&self, state: NavState) {

    }

    fn on_advance_to_next_step(&self, state: NavState) {

    }

    fn on_user_location_update(&self, location: UserLocation, state: NavState) {

    }
}
