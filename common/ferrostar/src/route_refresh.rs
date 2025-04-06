use std::time::SystemTime;

#[cfg(any(feature = "wasm-bindgen", test))]
use serde::{Deserialize, Serialize};
#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

#[cfg(feature = "alloc")]
use alloc::sync::Arc;

#[derive(Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub enum RouteRefreshStrategy {
    /// Never check for better routes.
    None,
    /// Check for better route at fixed time intervals.
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    Interval {
        /// The interval at which to check for better routes.
        interval_seconds: u64,
    },

    /// Use a custom strategy to determine when to check for better routes.
    #[cfg_attr(feature = "wasm-bindgen", serde(skip))]
    Custom {
        detector: Arc<dyn RouteRefreshDetector>,
    },
}

#[derive(Debug, Clone, PartialEq, Copy)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub enum RouteRefreshState {
    /// No route refresh needed.
    NoRefreshNeeded,
    /// Route refresh needed.
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    RefreshNeeded,
}

#[cfg_attr(feature = "uniffi", uniffi::export(with_foreign))]
pub trait RouteRefreshDetector: Send + Sync {
    /// Check if a route refresh is needed.
    fn check_refresh(&self, last_time_check: SystemTime) -> RouteRefreshState;
}

impl RouteRefreshStrategy {
    pub fn check_refresh(&self, last_time_check: SystemTime) -> RouteRefreshState {
        match self {
            RouteRefreshStrategy::None => RouteRefreshState::NoRefreshNeeded,

            RouteRefreshStrategy::Interval { interval_seconds } => {
                let elapsed_time = SystemTime::now()
                    .duration_since(last_time_check)
                    .unwrap_or_default();

                if elapsed_time.as_secs() >= *interval_seconds {
                    RouteRefreshState::RefreshNeeded
                } else {
                    RouteRefreshState::NoRefreshNeeded
                }
            }
            RouteRefreshStrategy::Custom { detector } => detector.check_refresh(last_time_check),
        }
    }
}
