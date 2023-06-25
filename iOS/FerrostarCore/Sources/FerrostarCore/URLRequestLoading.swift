import Foundation

/// A generic prodocol describing a network session capable of loading URL requests.
///
/// This exists to allow mocking in test code. A conformance is provided for `URLSession`,
/// which should be used for production code.
public protocol URLRequestLoading {
    func loadData(with urlRequest: URLRequest, completionHandler: @escaping (Data?, URLResponse?, Error?) -> Void)
}

extension URLSession: URLRequestLoading {
    public func loadData(with urlRequest: URLRequest, completionHandler: @escaping (Data?, URLResponse?, Error?) -> Void) {
        let task = dataTask(with: urlRequest) { (data, response, error) in
            completionHandler(data, response, error)
        }

        task.resume()
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
    private var urlResponseMap = Dictionary<URL, (Data?, URLResponse?)>()

    public func loadData(with urlRequest: URLRequest, completionHandler: (Data?, URLResponse?, Error?) -> Void) {
        guard let url = urlRequest.url else {
            completionHandler(nil, nil, MockURLSessionError.MissingURL)
            return
        }

        guard let (data, response) = urlResponseMap[url] else {
            completionHandler(nil, nil, MockURLSessionError.NoResponseMockForURL)
            return
        }

        completionHandler(data, response, nil)
    }

    /// Configures the session to answer all requests for the URL with the given data and response.
    public func registerMock(forURL url: URL, withData data: Data?, andResponse response: URLResponse) {
        urlResponseMap[url] = (data, response)
    }
}
