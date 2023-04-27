import FFI


public struct FerrostarCore {
    public init() {
    }

    // TODO: Some higher level wrapper that does something useful
    public func add(_ a: UInt32, _ b: UInt32) -> Int {
        Int(FFI.add(a: a, b: b))
    }
}
