import SnapshotTesting
import SwiftUI
import XCTest

extension XCTestCase {
    func assertView(
        named name: String? = nil,
        record: Bool = false,
        frame: CGSize = CGSize(width: 430, height: 932),
        @ViewBuilder content: () -> some View,
        file: StaticString = #file,
        testName: String = #function,
        line: UInt = #line
    ) {
        let view = content()
            .frame(width: frame.width, height: frame.height)

        assertSnapshot(of: view,
                       as: .image,
                       named: name,
                       record: record,
                       file: file,
                       testName: testName,
                       line: line)
    }
}
