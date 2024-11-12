import Foundation
import CarPlay

class NavigationTemplate: NSObject {

    public let template = CPMapTemplate()
    var navigationSession: CPNavigationSession?

//    required init(store: CarPlayStore) {
//        self.store = store

    override init() {
        super.init()

        // Build the primary CPMapTemplate
        self.buildTemplate()
    }
    
    func buildTemplate() {
        // Build the template
        template.automaticallyHidesNavigationBar = false
        template.tripEstimateStyle = .dark

        template.mapButtons = [
//            CarPlayUI.button(.zoomIn, action: {  }),
//            CarPlayUI.button(.zoomOut, action: { })
        ]
    }
}

extension MapTemplate: CPMapTemplateDelegate {

}
