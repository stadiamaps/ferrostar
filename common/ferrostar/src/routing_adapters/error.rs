#[cfg(feature = "alloc")]
use alloc::string::{String, ToString};

// TODO: This implementation seems less than ideal. In particular, it hides what sort of JSON error occurred due to an apparent bug in UniFFI.
// The trouble appears to be with generating "flat" enum bindings that are used with callback
// interfaces when the underlying actually has fields.
#[derive(Debug)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
pub enum InstantiationError {
    #[cfg_attr(
        feature = "std",
        error("Error parsing the JSON options for the request.")
    )]
    OptionsJsonParseError,
}

// TODO: See comment above
#[derive(Debug)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
pub enum RoutingRequestGenerationError {
    #[cfg_attr(
        feature = "std",
        error("Too few waypoints were provided to compute a route.")
    )]
    NotEnoughWaypoints,
    #[cfg_attr(feature = "std", error("Error generating JSON for the request."))]
    JsonError,
    #[cfg_attr(
        feature = "std",
        error("An unknown error generating a request was raised in foreign code.")
    )]
    UnknownRequestGenerationError,
}

#[cfg(feature = "uniffi")]
impl From<uniffi::UnexpectedUniFFICallbackError> for RoutingRequestGenerationError {
    fn from(_: uniffi::UnexpectedUniFFICallbackError) -> RoutingRequestGenerationError {
        RoutingRequestGenerationError::UnknownRequestGenerationError
    }
}

impl From<serde_json::Error> for InstantiationError {
    fn from(_: serde_json::Error) -> Self {
        InstantiationError::OptionsJsonParseError
    }
}

impl From<serde_json::Error> for RoutingRequestGenerationError {
    fn from(_: serde_json::Error) -> Self {
        RoutingRequestGenerationError::JsonError
    }
}

#[derive(Debug)]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
pub enum ParsingError {
    // TODO: Unable to find route and other common errors
    #[cfg_attr(feature = "std", error("Failed to parse route response: {error}."))]
    ParseError { error: String },
    #[cfg_attr(
        feature = "std",
        error("Routing adapter returned an unexpected status code: {code}.")
    )]
    InvalidStatusCode { code: String },
    #[cfg_attr(
        feature = "std",
        error("An unknown error parsing a response was raised in foreign code.")
    )]
    UnknownParsingError,
}

#[cfg(feature = "uniffi")]
impl From<uniffi::UnexpectedUniFFICallbackError> for ParsingError {
    fn from(_: uniffi::UnexpectedUniFFICallbackError) -> ParsingError {
        ParsingError::UnknownParsingError
    }
}

impl From<serde_json::Error> for ParsingError {
    fn from(e: serde_json::Error) -> Self {
        ParsingError::ParseError {
            error: e.to_string(),
        }
    }
}
