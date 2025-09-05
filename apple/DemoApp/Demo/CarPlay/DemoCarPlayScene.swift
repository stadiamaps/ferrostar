enum DemoCarPlayScene: Hashable {
    case search
    case navigation
}

extension DemoCarPlayScene: CustomStringConvertible {
    var description: String {
        switch self {
        case .search:
            ".search"
        case .navigation:
            ".navigation"
        }
    }
}
