/// Various helpers that generate views for previews.

import FerrostarCoreFFI
import Foundation

protocol TestFixtureFactory {
    associatedtype Output
    func build(_ n: Int) -> Output
}

extension TestFixtureFactory {
    func buildMany(_ n: Int) -> [Output] {
        (0 ... n).map { build($0) }
    }
}

struct VisualInstructionContentFactory: TestFixtureFactory {
    public init() {}

    public var textBuilder: (Int) -> String = { n in RoadNameFactory().build(n) }
    public func text(_ builder: @escaping (Int) -> String) -> Self {
        var copy = self
        copy.textBuilder = builder
        return copy
    }

    public func build(_ n: Int = 0) -> VisualInstructionContent {
        VisualInstructionContent(
            text: textBuilder(n),
            maneuverType: .turn,
            maneuverModifier: .left,
            roundaboutExitDegrees: nil,
            laneInfo: nil
        )
    }
}

struct VisualInstructionFactory: TestFixtureFactory {
    public init() {}

    public var primaryContentBuilder: (Int) -> VisualInstructionContent = { n in
        VisualInstructionContentFactory().build(n)
    }

    public var secondaryContentBuilder: (Int) -> VisualInstructionContent? = { _ in nil }

    public func secondaryContent(_ builder: @escaping (Int) -> VisualInstructionContent) -> Self {
        var copy = self
        copy.secondaryContentBuilder = builder
        return copy
    }

    public func build(_ n: Int = 0) -> VisualInstruction {
        VisualInstruction(
            primaryContent: primaryContentBuilder(n),
            secondaryContent: secondaryContentBuilder(n),
            subContent: nil,
            triggerDistanceBeforeManeuver: 42.0
        )
    }
}

struct RouteStepFactory: TestFixtureFactory {
    public init() {}
    public var visualInstructionBuilder: (Int) -> VisualInstruction = { n in VisualInstructionFactory().build(n) }
    public var roadNameBuilder: (Int) -> String = { n in RoadNameFactory().build(n) }

    public func build(_ n: Int = 0) -> RouteStep {
        RouteStep(
            geometry: [],
            distance: 100,
            duration: 99,
            roadName: roadNameBuilder(n),
            instruction: "Walk west on \(roadNameBuilder(n))",
            visualInstructions: [visualInstructionBuilder(n)],
            spokenInstructions: [],
            annotations: nil
        )
    }
}

struct RoadNameFactory: TestFixtureFactory {
    public init() {}
    public var baseNameBuilder: (Int) -> String = { _ in "Ave" }

    public func baseName(_ builder: @escaping (Int) -> String) -> Self {
        var copy = self
        copy.baseNameBuilder = builder
        return copy
    }

    public func build(_ n: Int = 0) -> String {
        let numberFormatter = NumberFormatter()
        numberFormatter.numberStyle = .ordinal
        return "\(numberFormatter.string(from: NSNumber(value: n + 1))!) \(baseNameBuilder(n))"
    }
}
