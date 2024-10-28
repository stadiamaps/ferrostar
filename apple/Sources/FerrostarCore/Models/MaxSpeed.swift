import Foundation

/// The OSRM formatted MaxSpeed. This is a custom field used by some API's like Mapbox,
/// Valhalla with OSRM json output, etc.
///
/// For more information see:
/// - https://wiki.openstreetmap.org/wiki/Key:maxspeed
/// - https://docs.mapbox.com/api/navigation/directions/#route-leg-object (search for `max_speed`)
/// - https://valhalla.github.io/valhalla/speeds/#assignment-of-speeds-to-roadways
public enum MaxSpeed: Codable, Equatable, Hashable {
    public enum Units: String, Codable {
        case kilometersPerHour = "km/h"
        case milesPerHour = "mph"
        case knots // "knots" are an option in core OSRM docs, though unsure if they're ever used in this context.
    }

    /// There is no speed limit (it's unlimited, e.g. German Autobahn)
    case noLimit

    /// The speed limit is not known.
    case unknown

    /// The speed limit is a known value and unit (this may be localized depending on the API).
    case speed(Double, unit: Units)

    enum CodingKeys: CodingKey {
        case none
        case unknown
        case speed
        case unit
    }

    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        if let none = try container.decodeIfPresent(Bool.self, forKey: .none),
           none == true
        {
            // The speed configuration is `{none: true}` for unlimited.
            self = .noLimit
        } else if let unknown = try container.decodeIfPresent(Bool.self, forKey: .unknown),
                  unknown == true
        {
            // The speed configuration is `{unknown: true}` for unknown.
            self = .unknown
        } else if let value = try container.decodeIfPresent(Double.self, forKey: .speed),
                  let unit = try container.decodeIfPresent(Units.self, forKey: .unit)
        {
            // The speed is a known value with units. Some API's may localize, others only support a single unit.
            self = .speed(value, unit: unit)
        } else {
            throw DecodingError.dataCorrupted(.init(
                codingPath: decoder.codingPath,
                debugDescription: "Invalid MaxSpeed, see docstrings for reference links"
            ))
        }
    }

    public func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)

        switch self {
        case .noLimit:
            try container.encode(true, forKey: .none)
        case .unknown:
            try container.encode(true, forKey: .unknown)
        case let .speed(value, unit: unit):
            try container.encode(value, forKey: .speed)
            try container.encode(unit, forKey: .unit)
        }
    }

    /// The MaxSpeed as a measurement
    public var measurementValue: Measurement<UnitSpeed>? {
        switch self {
        case .noLimit: .init(value: .infinity, unit: .kilometersPerHour)
        case .unknown: nil
        case let .speed(value, unit):
            switch unit {
            case .kilometersPerHour:
                .init(value: value, unit: .kilometersPerHour)
            case .milesPerHour:
                .init(value: value, unit: .milesPerHour)
            case .knots:
                .init(value: value, unit: .knots)
            }
        }
    }
}
