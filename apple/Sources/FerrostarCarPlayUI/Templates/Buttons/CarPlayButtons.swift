import CarPlay

struct CarPlayButtons {
    
    static func recenterButton(
        isHidden: Bool = false,
        isEnabled: Bool = true,
        _ action: @escaping () -> Void
    ) -> CPMapButton {
        var recenterButton = CPMapButton { (button) in
            action()
        }
        
        recenterButton.image = UIImage(systemName: "location.north.fill")
        recenterButton.isHidden = isHidden
        recenterButton.isEnabled = isEnabled
        
        return recenterButton
    }
}
