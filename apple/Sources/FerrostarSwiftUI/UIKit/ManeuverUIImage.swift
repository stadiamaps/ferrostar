import FerrostarCoreFFI
import UIKit

/// A UIImage representation of the maneuver image.
/// This is used for CarPlay, but may have other uses.
public class ManeuverUIImage {
    let name: ManeuverImageName

    public init(maneuverType: ManeuverType,
                maneuverModifier: ManeuverModifier?,
                drivingSide: DrivingSide = .right)
    {
        name = ManeuverImageName(maneuverType: maneuverType,
                                 maneuverModifier: maneuverModifier,
                                 drivingSide: drivingSide)
    }

    public var uiImage: UIImage? {
        let image = UIImage(named: name.value, in: .module, with: nil)
        return name.shouldMirror ? image?.withHorizontallyFlippedOrientation() : image
    }
}
