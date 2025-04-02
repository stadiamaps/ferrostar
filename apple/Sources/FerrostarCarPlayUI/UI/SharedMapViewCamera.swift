import Combine
import MapLibreSwiftUI

public class SharedMapViewCamera: ObservableObject {
    @Published public var camera: MapViewCamera
    
    public init(camera: MapViewCamera) {
        self.camera = camera
    }
}
