
public class ManeuverUIImage {
    let name = ManeuverImageName

    public init(maneuverType: ManeuverType, maneuverModifier: ManeuverModifier?) {
        name = ManeuverImageName(maneuverType: maneuverType, maneuverModifier: maneuverModifier)
    }

    public var uiImage: UIImage? {
        UIImage(named: name.value, in: .module)
    }
}
