import FerrostarCoreFFI
import Foundation

extension RouteRequest {
    var urlRequest: URLRequest {
        get throws {
            var urlRequest: URLRequest
            switch self {
            case let .httpPost(url: requestUrl, headers: headers, body: body):
                guard let url = URL(string: requestUrl) else {
                    throw FerrostarCoreError.invalidRequestUrl
                }

                urlRequest = URLRequest(url: url)
                urlRequest.httpMethod = "POST"
                for (header, value) in headers {
                    urlRequest.setValue(value, forHTTPHeaderField: header)
                }
                urlRequest.httpBody = Data(body)
            case let .httpGet(url: requestUrl, headers: headers):
                guard let url = URL(string: requestUrl) else {
                    throw FerrostarCoreError.invalidRequestUrl
                }

                urlRequest = URLRequest(url: url)
                urlRequest.httpMethod = "GET"
                for (header, value) in headers {
                    urlRequest.setValue(value, forHTTPHeaderField: header)
                }
            }

            urlRequest.timeoutInterval = 15

            return urlRequest
        }
    }
}
