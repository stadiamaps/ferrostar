import Foundation

/// Build a data provider that tells the UI whether to prefer a US style (MUTCD) or Vienna style speed limit sign.
protocol SpeedLimitStyleProviding {
    func useUSStyle() -> Bool
}

// TODO: Create a location based Provider using:
// MUTCD Signage Regions: https://en.wikipedia.org/wiki/Manual_on_Uniform_Traffic_Control_Devices
//    US, Canada, Mexico, Belize, Argentina, Bolivia, Brazil, Colombia, Equador, Guyana
//    Paraguay, Peru, Venezuela, Austrialia, Thailand.
// Region Codes: https://en.wikipedia.org/wiki/IETF_language_tag

/// Always prefer US Style (MUTCD)
class USSpeedLimitStyleProvider: SpeedLimitStyleProvider {
    func useUSStyle() -> Bool {
        true
    }
}

/// Always prefer Vienna Style
class SpeedLimitFixedToViennaConventionStyle: SpeedLimitStyleProvider {
    func useUSStyle() -> Bool {
        false
    }
}
