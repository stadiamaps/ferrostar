use std::sync::{Arc, Mutex};

use chrono::{Duration, Utc};

use crate::{models::{Route, UserLocation}, navigation_controller::models::NavState, navigation_session::{caching::models::{NavigationCachingConfig, NavigationSessionRecord}, NavigationObserver}};

pub mod models;

#[cfg_attr(feature = "uniffi", uniffi::export(with_foreign))]
pub trait NavigationCache: Send + Sync {
    fn save(&self, record: Vec<u8>);
    fn load(&self) -> Option<Vec<u8>>;
    fn delete(&self);
}

#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
struct NavigationSessionCache {
    config: NavigationCachingConfig,
    cache: Arc<dyn NavigationCache>,
    current_record: Mutex<Option<NavigationSessionRecord>>,
}

#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationSessionCache {

    #[cfg_attr(feature = "uniffi", uniffi::constructor)]
    pub fn new(config: NavigationCachingConfig, cache: Arc<dyn NavigationCache>) -> Self {
        Self {
            config,
            cache,
            current_record: Mutex::new(None),
        }
    }

    /// Check if the navigation session can be resumed.
    pub fn can_resume(&self) -> bool {
        self.current_record.lock().unwrap().is_some()
    }

    /// Load the navigation session record from the cache if it exists and is not stale.
    pub fn load(&self) -> Option<NavigationSessionRecord> {
        let record = self.cache.load()
            .and_then(|data| serde_json::from_slice(&data).ok())
            .and_then(|record: NavigationSessionRecord| {
                let age = Utc::now() - record.saved_at;
                let max_age = Duration::seconds(self.config.max_age_seconds);

                if age > max_age {
                    // The record is stale. Delete it from the cache.
                    self.cache.delete();
                    None
                } else {
                    Some(record)
                }
            });

        if let Some(current_record) = &record {
            self.current_record.lock().unwrap().replace(current_record.clone());
        }
        record
    }
}

#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationObserver for NavigationSessionCache {
    fn on_route_available(&self, route: Route) {
        if let Ok(mut record) = self.current_record.lock() {
            *record = Some(NavigationSessionRecord {
                saved_at: Utc::now(),
                route: route.clone(),
                trip_state: None,
            });
        }
        // Ignore caching, that first cache is handled by the on_get_initial_state method
    }

    fn on_get_initial_state(&self, state: NavState) {
        self.handle_update(state, true)
    }

    fn on_advance_to_next_step(&self, state: NavState) {
        self.handle_update(state, true)
    }

    fn on_user_location_update(
        &self,
        #[allow(unused_variables)] location: UserLocation,
        state: NavState
    ) {
        let should_cache = self.current_record.lock()
            .ok()
            .and_then(|record| {
                record.as_ref().map(|record| {
                    let elapsed = Utc::now() - record.saved_at;
                    let interval = Duration::seconds(self.config.cache_interval_seconds);
                    elapsed > interval
                })
            })
            .unwrap_or(false);

        self.handle_update(state, should_cache);
    }
}

impl NavigationSessionCache {
    fn handle_update(&self, state: NavState, should_cache: bool) {
        if !should_cache {
            return
        }

        let record_to_cache = self.current_record.lock()
            .ok()
            .and_then(|mut record| {
                record.as_mut().map(|record| {
                    record.saved_at = Utc::now();
                    record.trip_state = Some(state.trip_state());
                    record.clone() // Clone for serialization
                })
            })
            .and_then(|record| serde_json::to_vec(&record).ok());

        if let Some(record) = record_to_cache {
            self.cache.save(record);
        }
    }
}
