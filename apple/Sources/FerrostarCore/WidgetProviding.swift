import FerrostarCoreFFI

public protocol WidgetProviding {
    func update(visualInstruction: VisualInstruction, tripProgress: TripProgress)
    func terminate()
}
