import FerrostarCoreFFI
import Foundation

extension RouteRequest {
    var urlRequest: URLRequest {
        get throws {
            var urlRequest: URLRequest
            let requestHeaders: [String: String]
            switch self {
            case let .httpPost(url: requestUrl, headers: headers, body: body):
                guard let url = URL(string: requestUrl) else {
                    throw FerrostarCoreError.invalidRequestUrl
                }

                requestHeaders = headers

                urlRequest = URLRequest(url: url)
                urlRequest.httpMethod = "POST"
                urlRequest.httpBody = Data(body)
            case let .httpGet(url: requestUrl, headers: headers):
                guard let url = URL(string: requestUrl) else {
                    throw FerrostarCoreError.invalidRequestUrl
                }
                requestHeaders = headers

                urlRequest = URLRequest(url: url)
                urlRequest.httpMethod = "GET"
            }

            for (header, value) in requestHeaders {
                urlRequest.setValue(value, forHTTPHeaderField: header)
            }

            urlRequest.timeoutInterval = 15

            return urlRequest
        }
    }
}
