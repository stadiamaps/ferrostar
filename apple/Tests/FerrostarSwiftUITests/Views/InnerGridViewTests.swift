import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class InnerGridViewTests: XCTestCase {
    func testFullView() {
        assertView {
            InnerGridView(
                topLeading: {
                    Rectangle().frame(width: 64, height: 64)
                        .foregroundColor(.blue)
                },
                topCenter: {
                    Rectangle()
                        .foregroundColor(.blue)
                },
                topTrailing: {
                    Rectangle().frame(height: 64)
                        .foregroundColor(.blue)
                },
                midLeading: {
                    Rectangle().frame(width: 64)
                        .foregroundColor(.red)
                },
                midTrailing: {
                    Rectangle()
                        .foregroundColor(.red)
                },
                bottomLeading: {
                    Rectangle().frame(width: 64, height: 64)
                        .foregroundColor(.blue)
                },
                bottomCenter: {
                    Rectangle()
                        .foregroundColor(.blue)
                },
                bottomTrailing: {
                    Rectangle().frame(height: 64)
                        .foregroundColor(.blue)
                }
            )
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.green)
        }
    }

    func testLimitedView() {
        assertView {
            InnerGridView(
                topTrailing: {
                    Rectangle().frame(width: 64, height: 64)
                        .foregroundColor(.blue)
                },
                bottomLeading: {
                    Rectangle().frame(width: 64, height: 64)
                        .foregroundColor(.blue)
                }
            )
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.green)
        }
    }

    func testRightToLeftView() {
        assertView {
            InnerGridView(
                topTrailing: {
                    Rectangle().frame(width: 64, height: 64)
                        .foregroundColor(.blue)
                },
                bottomLeading: {
                    Rectangle().frame(width: 64, height: 64)
                        .foregroundColor(.blue)
                }
            )
            .environment(\.layoutDirection, .rightToLeft)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.green)
        }
    }
}
