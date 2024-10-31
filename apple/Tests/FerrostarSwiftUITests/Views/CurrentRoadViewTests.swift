import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

struct CustomRoadNameViewTheme: RoadNameViewTheme {
    let textColor: Color = .white
    let textFont: Font = .headline
    let backgroundColor: Color = .green
    let borderColor: Color = .yellow
}

final class CurrentRoadViewTests: XCTestCase {
    func testDefaultCurrentRoadView() {
        assertView {
            CurrentRoadNameView(currentRoadName: "Sesame Street")
        }
    }

    func testDefaultCurrentRoadViewFunkyStyle() {
        assertView {
            CurrentRoadNameView(currentRoadName: "Sesame Street")
                .theme(CustomRoadNameViewTheme())
                .borderWidth(6)
                .padding(EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16))
                .shape(RoundedRectangle(cornerRadius: 16))
        }
    }

    func testDefaultCurrentRoadViewNil() {
        assertView {
            CurrentRoadNameView(currentRoadName: nil)
        }
    }

    func testDefaultCurrentRoadViewBlank() {
        assertView {
            CurrentRoadNameView(currentRoadName: "")
        }
    }
}
