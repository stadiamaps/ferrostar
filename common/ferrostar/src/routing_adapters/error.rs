use uniffi::UnexpectedUniFFICallbackError;

// TODO: This implementation seems less than ideal. In particular, it hides what sort of JSON error occurred due to an apparent bug in UniFFI.
// The trouble appears to be with generating "flat" enum bindings that are used with callback
// interfaces when the underlying actually has fields.
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum InstantiationError {
    #[error("Error generating JSON for the request.")]
    JsonError, }

// TODO: See comment above
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum RoutingRequestGenerationError {
    #[error("Too few waypoints were provided to compute a route.")]
    NotEnoughWaypoints,
    #[error("Error generating JSON for the request.")]
    JsonError,
    #[error("An unknown error generating a request was raised in foreign code.")]
    UnknownError,
}

impl From<UnexpectedUniFFICallbackError> for RoutingRequestGenerationError {
    fn from(_: UnexpectedUniFFICallbackError) -> RoutingRequestGenerationError {
        RoutingRequestGenerationError::UnknownError
    }
}

impl From<serde_json::Error> for InstantiationError {
    fn from(_: serde_json::Error) -> Self {
        InstantiationError::JsonError
    }
}

impl From<serde_json::Error> for RoutingRequestGenerationError {
    fn from(_: serde_json::Error) -> Self {
        RoutingRequestGenerationError::JsonError
    }
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum RoutingResponseParseError {
    // TODO: Unable to find route and other common errors
    #[error("Failed to parse route response: {error}.")]
    ParseError { error: String },
    #[error("An unknown error parsing a response was raised in foreign code.")]
    UnknownError,
}

impl From<UnexpectedUniFFICallbackError> for RoutingResponseParseError {
    fn from(_: UnexpectedUniFFICallbackError) -> RoutingResponseParseError {
        RoutingResponseParseError::UnknownError
    }
}

impl From<serde_json::Error> for RoutingResponseParseError {
    fn from(e: serde_json::Error) -> Self {
        RoutingResponseParseError::ParseError {
            error: e.to_string(),
        }
    }
}
