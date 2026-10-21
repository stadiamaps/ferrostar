#[cfg(feature = "uniffi")]
use std::sync::{Arc, Mutex};

#[cfg(feature = "uniffi")]
use chrono::{Duration, Utc};

#[cfg(feature = "uniffi")]
use crate::{
    models::{Route, UserLocation},
    navigation_controller::models::NavState,
    navigation_session::{
        NavigationObserver,
        caching::models::{NavigationCachingConfig, NavigationSessionSnapshot},
    },
};

pub mod models;

#[cfg(feature = "uniffi")]
#[cfg_attr(feature = "uniffi", uniffi::export(with_foreign))]
pub trait NavigationCache: Send + Sync {
    fn save(&self, record: Vec<u8>);
    fn load(&self) -> Option<Vec<u8>>;
    fn delete(&self);
}

#[cfg(feature = "uniffi")]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
struct NavigationSessionCache {
    config: NavigationCachingConfig,
    cache: Arc<dyn NavigationCache>,
    current_record: Mutex<Option<NavigationSessionSnapshot>>,
}

#[cfg(feature = "uniffi")]
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

    /// Check if the cache holds a snapshot that a resumed session can start from.
    ///
    /// It reads and decodes the stored snapshot on every call, so use it sparingly.
    pub fn can_resume(&self) -> bool {
        matches!(
            self.read_stored_snapshot(),
            StoredSnapshot::Fresh(NavigationSessionSnapshot {
                trip_state: Some(_),
                ..
            })
        )
    }

    /// Load the navigation session record from the cache if it exists and is not stale.
    ///
    /// Deletes the stored state if it is stale or invalid.
    pub fn load(&self) -> Option<NavigationSessionSnapshot> {
        match self.read_stored_snapshot() {
            StoredSnapshot::Missing => None,
            StoredSnapshot::Stale | StoredSnapshot::Undecodable => {
                self.cache.delete();
                None
            }
            StoredSnapshot::Fresh(record) => {
                if let Ok(mut current_record) = self.current_record.lock() {
                    current_record.replace(record.clone());
                }
                Some(record)
            }
        }
    }
}

/// The result of reading the platform cache, before any action on it.
#[cfg(feature = "uniffi")]
// The value only lives for one call, so the size of `Fresh` is not a concern.
#[allow(clippy::large_enum_variant)]
enum StoredSnapshot {
    Missing,
    /// The cache holds bytes that do not decode to a [`NavigationSessionSnapshot`].
    /// For example, the record was written by an incompatible version of Ferrostar.
    Undecodable,
    /// The record is older than [`NavigationCachingConfig::max_age_seconds`].
    Stale,
    Fresh(NavigationSessionSnapshot),
}

#[cfg(feature = "uniffi")]
#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationObserver for NavigationSessionCache {
    fn on_get_initial_state(&self, state: NavState) {
        self.handle_update(state, true);
    }

    fn on_user_location_update(
        &self,
        #[allow(unused_variables)] location: UserLocation,
        state: NavState,
    ) {
        let should_cache = self
            .current_record
            .lock()
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

    fn on_advance_to_next_step(&self, state: NavState) {
        self.handle_update(state, true);
    }

    fn on_route_available(&self, route: Route) {
        if let Ok(mut record) = self.current_record.lock() {
            *record = Some(NavigationSessionSnapshot {
                saved_at: Utc::now(),
                route: route.clone(),
                trip_state: None,
            });
        }
        // We don't need to cache the snapshot here because it will be cached immediately after
        // when on_get_initial_state is called. See `NavigationSession` for observer sequences.
    }
}

#[cfg(feature = "uniffi")]
impl NavigationSessionCache {
    fn read_stored_snapshot(&self) -> StoredSnapshot {
        let Some(data) = self.cache.load() else {
            return StoredSnapshot::Missing;
        };
        let Ok(record) = serde_json::from_slice::<NavigationSessionSnapshot>(&data) else {
            return StoredSnapshot::Undecodable;
        };

        let age = Utc::now() - record.saved_at;
        if age > Duration::seconds(self.config.max_age_seconds) {
            StoredSnapshot::Stale
        } else {
            StoredSnapshot::Fresh(record)
        }
    }

    fn handle_update(&self, state: NavState, should_cache: bool) {
        if !should_cache {
            return;
        }

        let record_to_cache = self
            .current_record
            .lock()
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

#[cfg(all(test, feature = "uniffi"))]
mod tests {
    use super::*;
    use crate::navigation_controller::models::TripState;
    use crate::navigation_controller::test_helpers::{gen_dummy_route_step, gen_route_from_steps};
    use proptest::prelude::*;
    use std::sync::atomic::{AtomicUsize, Ordering};

    const MAX_AGE_SECONDS: i64 = 60 * 60;

    /// An in-memory stand-in for a platform cache (`UserDefaults`, `SharedPreferences`, ...).
    #[derive(Default)]
    struct InMemoryCache {
        data: Mutex<Option<Vec<u8>>>,
        delete_count: AtomicUsize,
    }

    impl NavigationCache for InMemoryCache {
        fn save(&self, record: Vec<u8>) {
            *self.data.lock().unwrap() = Some(record);
        }

        fn load(&self) -> Option<Vec<u8>> {
            self.data.lock().unwrap().clone()
        }

        fn delete(&self) {
            self.delete_count.fetch_add(1, Ordering::SeqCst);
            *self.data.lock().unwrap() = None;
        }
    }

    impl InMemoryCache {
        fn deletes(&self) -> usize {
            self.delete_count.load(Ordering::SeqCst)
        }
    }

    fn session_cache(store: &Arc<InMemoryCache>) -> NavigationSessionCache {
        NavigationSessionCache::new(
            NavigationCachingConfig {
                cache_interval_seconds: 5,
                max_age_seconds: MAX_AGE_SECONDS,
            },
            store.clone(),
        )
    }

    fn route() -> Route {
        gen_route_from_steps(vec![gen_dummy_route_step(0.0, 0.0, 0.001, 0.001)])
    }

    fn idle_trip_state() -> TripState {
        NavState::idle(None).trip_state()
    }

    /// Run the observer calls that `NavigationSession::get_initial_state` makes.
    /// This saves a snapshot to the platform cache.
    fn start_session(cache: &NavigationSessionCache) {
        cache.on_route_available(route());
        cache.on_get_initial_state(NavState::idle(None));
    }

    fn store_snapshot(store: &InMemoryCache, age: Duration, trip_state: Option<TripState>) {
        let snapshot = NavigationSessionSnapshot {
            saved_at: Utc::now() - age,
            route: route(),
            trip_state,
        };
        store.save(serde_json::to_vec(&snapshot).unwrap());
    }

    #[test]
    fn empty_cache_cannot_resume() {
        let store = Arc::new(InMemoryCache::default());
        let cache = session_cache(&store);

        assert!(!cache.can_resume());
        assert!(cache.load().is_none());
        assert_eq!(store.deletes(), 0);
    }

    #[test]
    fn can_resume_during_active_session() {
        let store = Arc::new(InMemoryCache::default());
        let cache = session_cache(&store);

        start_session(&cache);

        assert!(cache.can_resume());
    }

    /// A new process has an empty in-memory record,
    /// but it must still find the snapshot that the last process saved.
    #[test]
    fn can_resume_after_process_restart() {
        let store = Arc::new(InMemoryCache::default());
        start_session(&session_cache(&store));

        let cache = session_cache(&store);

        assert!(cache.can_resume());
        let snapshot = cache.load().expect("a fresh snapshot loads");
        assert!(snapshot.trip_state.is_some());
    }

    /// The app deletes the snapshot through its own `NavigationCache` reference.
    /// The in-memory record must not keep the deleted snapshot resumable.
    #[test]
    fn cannot_resume_after_platform_cache_delete() {
        let store = Arc::new(InMemoryCache::default());
        let cache = session_cache(&store);
        start_session(&cache);

        store.delete();

        assert!(!cache.can_resume());
        assert!(cache.load().is_none());
    }

    #[test]
    fn stale_snapshot_cannot_resume_and_load_deletes_it() {
        let store = Arc::new(InMemoryCache::default());
        store_snapshot(
            &store,
            Duration::seconds(MAX_AGE_SECONDS + 60),
            Some(idle_trip_state()),
        );
        let cache = session_cache(&store);

        assert!(!cache.can_resume());
        assert_eq!(store.deletes(), 0, "can_resume does not change the cache");

        assert!(cache.load().is_none());
        assert_eq!(store.deletes(), 1);
        assert!(store.load().is_none());
    }

    #[test]
    fn undecodable_snapshot_cannot_resume_and_load_deletes_it() {
        let store = Arc::new(InMemoryCache::default());
        store.save(b"{\"not\": \"a snapshot\"}".to_vec());
        let cache = session_cache(&store);

        assert!(!cache.can_resume());
        assert_eq!(store.deletes(), 0, "can_resume does not change the cache");

        assert!(cache.load().is_none());
        assert_eq!(store.deletes(), 1);
        assert!(store.load().is_none());
    }

    /// `can_resume` is false without a trip state, but `load` still returns the record.
    #[test]
    fn snapshot_without_trip_state_cannot_resume() {
        let store = Arc::new(InMemoryCache::default());
        store_snapshot(&store, Duration::zero(), None);
        let cache = session_cache(&store);

        assert!(!cache.can_resume());
        let snapshot = cache.load().expect("a fresh record loads");
        assert!(snapshot.trip_state.is_none());
        assert_eq!(store.deletes(), 0);
    }

    /// A snapshot that `load` returns stays in the cache,
    /// so the app can check again (for example, after a configuration change).
    #[test]
    fn load_keeps_a_fresh_snapshot() {
        let store = Arc::new(InMemoryCache::default());
        store_snapshot(&store, Duration::zero(), Some(idle_trip_state()));
        let cache = session_cache(&store);

        assert!(cache.load().is_some());
        assert!(cache.can_resume());
        assert_eq!(store.deletes(), 0);
    }

    proptest! {
        /// `can_resume` is true exactly when `load` returns a snapshot with a trip state.
        #[test]
        fn can_resume_agrees_with_load(
            // Skip ages near `max_age_seconds`.
            // A record at the boundary can go stale between the two calls.
            age_seconds in prop_oneof![
                -MAX_AGE_SECONDS..(MAX_AGE_SECONDS - 10),
                (MAX_AGE_SECONDS + 10)..(MAX_AGE_SECONDS * 48),
            ],
            has_trip_state: bool,
        ) {
            let store = Arc::new(InMemoryCache::default());
            store_snapshot(
                &store,
                Duration::seconds(age_seconds),
                has_trip_state.then(idle_trip_state),
            );
            let cache = session_cache(&store);

            let can_resume = cache.can_resume();
            let loaded = cache.load();

            prop_assert_eq!(
                can_resume,
                loaded.is_some_and(|snapshot| snapshot.trip_state.is_some())
            );
        }
    }
}
