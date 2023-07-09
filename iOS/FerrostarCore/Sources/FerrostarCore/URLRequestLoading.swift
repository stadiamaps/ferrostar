import Foundation

/// A generic prodocol describing a network session capable of loading URL requests.
///
/// This exists to allow mocking in test code. A conformance is provided for `URLSession`,
/// which should be used for production code.
public protocol URLRequestLoading {
    func loadData(with urlRequest: URLRequest) async throws -> (Data, URLResponse)
}

extension URLSession: URLRequestLoading {
    public func loadData(with urlRequest: URLRequest) async throws -> (Data, URLResponse) {
        try await data(for: urlRequest)
    }
}

enum MockURLSessionError: Error {
    /// The URLRequest has no URL
    case MissingURL
    /// No response has been mocked for the given URL
    case NoResponseMockForURL
}

/// Mocks network responses by URL. Super quick-and-dirty for testing with mocks.
///
/// By default, it will return an error for all requests. Register a mock by URL with ``registerMock(forURL:withData:andResponse:)``
public class MockURLSession: URLRequestLoading {
    private var urlResponseMap = Dictionary<URL, (Data, URLResponse)>()

    public func loadData(with urlRequest: URLRequest) async throws -> (Data, URLResponse) {
        guard let url = urlRequest.url else {
            throw MockURLSessionError.MissingURL
        }

        guard let (data, response) = urlResponseMap[url] else {
            throw MockURLSessionError.NoResponseMockForURL
        }

        return (data, response)
    }

    /// Configures the session to answer all requests for the URL with the given data and response.
    public func registerMock(forURL url: URL, withData data: Data, andResponse response: URLResponse) {
        urlResponseMap[url] = (data, response)
    }
}
