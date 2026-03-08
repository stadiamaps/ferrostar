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
    var textBuilder: (Int) -> String = { n in RoadNameFactory().build(n) }
    func text(_ builder: @escaping (Int) -> String) -> Self {
        var copy = self
        copy.textBuilder = builder
        return copy
    }

    func build(_ n: Int = 0) -> VisualInstructionContent {
        VisualInstructionContent(
            text: textBuilder(n),
            maneuverType: .turn,
            maneuverModifier: .left,
            roundaboutExitDegrees: nil,
            laneInfo: nil,
            exitNumbers: []
        )
    }
}

struct VisualInstructionFactory: TestFixtureFactory {
    var primaryContentBuilder: (Int) -> VisualInstructionContent = { n in
        VisualInstructionContentFactory().build(n)
    }

    var secondaryContentBuilder: (Int) -> VisualInstructionContent? = { _ in nil }

    func secondaryContent(_ builder: @escaping (Int) -> VisualInstructionContent) -> Self {
        var copy = self
        copy.secondaryContentBuilder = builder
        return copy
    }

    func build(_ n: Int = 0) -> VisualInstruction {
        VisualInstruction(
            primaryContent: primaryContentBuilder(n),
            secondaryContent: secondaryContentBuilder(n),
            subContent: nil,
            triggerDistanceBeforeManeuver: 42.0
        )
    }
}

struct RouteStepFactory: TestFixtureFactory {
    var visualInstructionBuilder: (Int) -> VisualInstruction = { n in
        VisualInstructionFactory().build(n)
    }

    var roadNameBuilder: (Int) -> String = { n in RoadNameFactory().build(n) }

    func build(_ n: Int = 0) -> RouteStep {
        RouteStep(
            geometry: [],
            distance: 100,
            duration: 99,
            roadName: roadNameBuilder(n),
            exits: [],
            instruction: "Walk west on \(roadNameBuilder(n))",
            visualInstructions: [visualInstructionBuilder(n)],
            spokenInstructions: [],
            annotations: nil,
            incidents: [],
            drivingSide: .left,
            roundaboutExitNumber: nil
        )
    }
}

struct RoadNameFactory: TestFixtureFactory { var baseNameBuilder: (Int) -> String = { _ in "Ave" }

    func baseName(_ builder: @escaping (Int) -> String) -> Self {
        var copy = self
        copy.baseNameBuilder = builder
        return copy
    }

    func build(_ n: Int = 0) -> String {
        let numberFormatter = NumberFormatter()
        numberFormatter.numberStyle = .ordinal
        return "\(numberFormatter.string(from: NSNumber(value: n + 1))!) \(baseNameBuilder(n))"
    }
}
