import FerrostarSwiftUI
import SnapshotTesting
import SwiftUI
import XCTest

public extension XCTestCase {
    func assertView(
        named name: String? = nil,
        record: Bool = false,
        colorScheme: ColorScheme = .light,
        navigationUITheme: NavigationUITheme = DefaultNavigationUITheme(),
        navigationFormatterCollection: FormatterCollection = TestingFormatterCollection(),
        frame: CGSize = CGSize(width: 430, height: 932),
        @ViewBuilder content: () -> some View,
        file: StaticString = #file,
        testName: String = #function,
        line: UInt = #line
    ) {
        let view = content()
            .environment(\.colorScheme, colorScheme)
            .environment(\.navigationUITheme, navigationUITheme)
            .environment(\.navigationFormatterCollection, navigationFormatterCollection)
            .frame(width: frame.width, height: frame.height)
            .background(Color(red: 130 / 255, green: 203 / 255, blue: 114 / 255))

        assertSnapshot(of: view,
                       as: .image(precision: 0.99),
                       named: name,
                       record: record,
                       file: file,
                       testName: testName,
                       line: line)
    }
}
