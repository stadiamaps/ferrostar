import SwiftUI
import XCTest
import SnapshotTesting
@testable import FerrostarCoreFFI
@testable import FerrostarMapLibreUI

final class ManeuverImageTests: XCTestCase {
    
    func testManeuverImageDefaultTheme() {
        assertView {
            ManeuverImage(maneuverType: .turn, maneuverModifier: .right)
                .frame(width: 128, height: 128)
        }
        
        assertView {
            ManeuverImage(maneuverType: .fork, maneuverModifier: .left)
                .frame(width: 32)
        }
    }
    
    func testManeuverImageCustomColor() {
        assertView {
            ManeuverImage(maneuverType: .merge, maneuverModifier: .slightLeft)
                .frame(width: 92)
                .foregroundColor(.blue)
        }
    }
    
    func testManeuverImageDoesNotExist() {
        assertView {
            ManeuverImage(maneuverType: .arrive, maneuverModifier: .slightLeft)
                .frame(width: 92)
                .foregroundColor(.white)
                .background(Color.green)
        }
    }
}
