import FerrostarCoreFFI
import UIKit

/// A UIImage representation of the maneuver image.
/// This is used for CarPlay, but may have other uses.
public class ManeuverUIImage {
    let name: ManeuverImageName

    public init(maneuverType: ManeuverType, maneuverModifier: ManeuverModifier?) {
        name = ManeuverImageName(maneuverType: maneuverType, maneuverModifier: maneuverModifier)
    }

    public var uiImage: UIImage? {
        UIImage(named: name.value, in: .module, with: nil)
    }
}
