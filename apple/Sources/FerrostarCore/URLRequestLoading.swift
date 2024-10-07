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
    case missingURL
    /// No response has been mocked for the given method and URL
    case noResponseMockForMethodAndURL
}

/// Mocks network responses by URL. Super quick-and-dirty for testing with mocks.
///
/// By default, it will return an error for all requests. Register a mock by URL with
/// ``registerMock(forMethod:andURL:withData:andResponse:)``.
public class MockURLSession: URLRequestLoading {
    private var urlResponseMap = [String: [URL: (Data, URLResponse)]]()

    public func loadData(with urlRequest: URLRequest) async throws -> (Data, URLResponse) {
        let method = urlRequest.httpMethod ?? "GET"

        guard let url = urlRequest.url else {
            throw MockURLSessionError.missingURL
        }

        guard let (data, response) = urlResponseMap[method]?[url] else {
            throw MockURLSessionError.noResponseMockForMethodAndURL
        }

        return (data, response)
    }

    /// Configures the session to answer all requests for the URL with the given data and response.
    public func registerMock(
        forMethod method: String,
        andURL url: URL,
        withData data: Data,
        andResponse response: URLResponse
    ) {
        var map = urlResponseMap[method] ?? [:]

        map[url] = (data, response)
        urlResponseMap[method] = map
    }
}
