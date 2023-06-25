import XCTest
@testable import FerrostarCore

final class MockURLSessionTests: XCTestCase {
    func testUninitializedSession() throws {
        let exp = expectation(description: "URLRequest for an unregistered URL should result in an error (no configuration has been done)")

        let session = MockURLSession()
        session.loadData(with: URLRequest(url: URL(string: "https://example.com/")!)) { data, response, error in
            XCTAssertNil(data)
            XCTAssertNil(response)
            XCTAssertEqual(error as! MockURLSessionError, MockURLSessionError.NoResponseMockForURL)
            exp.fulfill()
        }

        wait(for: [exp], timeout: 1.0)
    }

    func testMockedURL() throws {
        let expUnRegistered = expectation(description: "URLRequest should return the expcted data for a mocked URL")
        let expRegistered = expectation(description: "URLRequest for an unregistered URL should result in an error (configuration has been done, but not for this URL)")

        let url = URL(string: "https://example.com/registered")!
        let mockData = "foobar".data(using: .utf8)!
        let mockResponse = HTTPURLResponse(url: url, statusCode: 200, httpVersion: "HTTP/1.1", headerFields: nil)!

        let session = MockURLSession()

        session.registerMock(forURL: url, withData: mockData, andResponse: mockResponse)

        session.loadData(with: URLRequest(url: url)) { data, response, error in
            XCTAssertEqual(data, mockData)
            XCTAssertEqual(response, mockResponse)
            XCTAssertNil(error)
            expRegistered.fulfill()
        }

        session.loadData(with: URLRequest(url: URL(string: "https://example.com/unregistered")!)) { data, response, error in
            XCTAssertNil(data)
            XCTAssertNil(response)
            XCTAssertEqual(error as! MockURLSessionError, MockURLSessionError.NoResponseMockForURL)
            expUnRegistered.fulfill()
        }

        wait(for: [expRegistered, expUnRegistered], timeout: 1.0)
    }
}
