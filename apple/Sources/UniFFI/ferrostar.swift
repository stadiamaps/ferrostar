// swiftlint:disable all
import Foundation

// Depending on the consumer's build setup, the low-level FFI code
// might be in a separate module, or it might be compiled inline into
// this module. This is a bit of light hackery to work with both.
#if canImport(ferrostarFFI)
    import ferrostarFFI
#endif

private extension RustBuffer {
    // Allocate a new buffer, copying the contents of a `UInt8` array.
    init(bytes: [UInt8]) {
        let rbuf = bytes.withUnsafeBufferPointer { ptr in
            RustBuffer.from(ptr)
        }
        self.init(capacity: rbuf.capacity, len: rbuf.len, data: rbuf.data)
    }

    static func empty() -> RustBuffer {
        RustBuffer(capacity: 0, len: 0, data: nil)
    }

    static func from(_ ptr: UnsafeBufferPointer<UInt8>) -> RustBuffer {
        try! rustCall { ffi_ferrostar_rustbuffer_from_bytes(ForeignBytes(bufferPointer: ptr), $0) }
    }

    // Frees the buffer in place.
    // The buffer must not be used after this is called.
    func deallocate() {
        try! rustCall { ffi_ferrostar_rustbuffer_free(self, $0) }
    }
}

private extension ForeignBytes {
    init(bufferPointer: UnsafeBufferPointer<UInt8>) {
        self.init(len: Int32(bufferPointer.count), data: bufferPointer.baseAddress)
    }
}

// For every type used in the interface, we provide helper methods for conveniently
// lifting and lowering that type from C-compatible data, and for reading and writing
// values of that type in a buffer.

// Helper classes/extensions that don't change.
// Someday, this will be in a library of its own.

private extension Data {
    init(rustBuffer: RustBuffer) {
        self.init(
            bytesNoCopy: rustBuffer.data!,
            count: Int(rustBuffer.len),
            deallocator: .none
        )
    }
}

// Define reader functionality.  Normally this would be defined in a class or
// struct, but we use standalone functions instead in order to make external
// types work.
//
// With external types, one swift source file needs to be able to call the read
// method on another source file's FfiConverter, but then what visibility
// should Reader have?
// - If Reader is fileprivate, then this means the read() must also
//   be fileprivate, which doesn't work with external types.
// - If Reader is internal/public, we'll get compile errors since both source
//   files will try define the same type.
//
// Instead, the read() method and these helper functions input a tuple of data

private func createReader(data: Data) -> (data: Data, offset: Data.Index) {
    (data: data, offset: 0)
}

// Reads an integer at the current offset, in big-endian order, and advances
// the offset on success. Throws if reading the integer would move the
// offset past the end of the buffer.
private func readInt<T: FixedWidthInteger>(_ reader: inout (data: Data, offset: Data.Index)) throws -> T {
    let range = reader.offset ..< reader.offset + MemoryLayout<T>.size
    guard reader.data.count >= range.upperBound else {
        throw UniffiInternalError.bufferOverflow
    }
    if T.self == UInt8.self {
        let value = reader.data[reader.offset]
        reader.offset += 1
        return value as! T
    }
    var value: T = 0
    let _ = withUnsafeMutableBytes(of: &value) { reader.data.copyBytes(to: $0, from: range) }
    reader.offset = range.upperBound
    return value.bigEndian
}

// Reads an arbitrary number of bytes, to be used to read
// raw bytes, this is useful when lifting strings
private func readBytes(_ reader: inout (data: Data, offset: Data.Index), count: Int) throws -> [UInt8] {
    let range = reader.offset ..< (reader.offset + count)
    guard reader.data.count >= range.upperBound else {
        throw UniffiInternalError.bufferOverflow
    }
    var value = [UInt8](repeating: 0, count: count)
    value.withUnsafeMutableBufferPointer { buffer in
        reader.data.copyBytes(to: buffer, from: range)
    }
    reader.offset = range.upperBound
    return value
}

// Reads a float at the current offset.
private func readFloat(_ reader: inout (data: Data, offset: Data.Index)) throws -> Float {
    try Float(bitPattern: readInt(&reader))
}

// Reads a float at the current offset.
private func readDouble(_ reader: inout (data: Data, offset: Data.Index)) throws -> Double {
    try Double(bitPattern: readInt(&reader))
}

// Indicates if the offset has reached the end of the buffer.
private func hasRemaining(_ reader: (data: Data, offset: Data.Index)) -> Bool {
    reader.offset < reader.data.count
}

// Define writer functionality.  Normally this would be defined in a class or
// struct, but we use standalone functions instead in order to make external
// types work.  See the above discussion on Readers for details.

private func createWriter() -> [UInt8] {
    []
}

private func writeBytes(_ writer: inout [UInt8], _ byteArr: some Sequence<UInt8>) {
    writer.append(contentsOf: byteArr)
}

// Writes an integer in big-endian order.
//
// Warning: make sure what you are trying to write
// is in the correct type!
private func writeInt(_ writer: inout [UInt8], _ value: some FixedWidthInteger) {
    var value = value.bigEndian
    withUnsafeBytes(of: &value) { writer.append(contentsOf: $0) }
}

private func writeFloat(_ writer: inout [UInt8], _ value: Float) {
    writeInt(&writer, value.bitPattern)
}

private func writeDouble(_ writer: inout [UInt8], _ value: Double) {
    writeInt(&writer, value.bitPattern)
}

// Protocol for types that transfer other types across the FFI. This is
// analogous to the Rust trait of the same name.
private protocol FfiConverter {
    associatedtype FfiType
    associatedtype SwiftType

    static func lift(_ value: FfiType) throws -> SwiftType
    static func lower(_ value: SwiftType) -> FfiType
    static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType
    static func write(_ value: SwiftType, into buf: inout [UInt8])
}

// Types conforming to `Primitive` pass themselves directly over the FFI.
private protocol FfiConverterPrimitive: FfiConverter where FfiType == SwiftType {}

extension FfiConverterPrimitive {
    public static func lift(_ value: FfiType) throws -> SwiftType {
        value
    }

    public static func lower(_ value: SwiftType) -> FfiType {
        value
    }
}

// Types conforming to `FfiConverterRustBuffer` lift and lower into a `RustBuffer`.
// Used for complex types where it's hard to write a custom lift/lower.
private protocol FfiConverterRustBuffer: FfiConverter where FfiType == RustBuffer {}

extension FfiConverterRustBuffer {
    public static func lift(_ buf: RustBuffer) throws -> SwiftType {
        var reader = createReader(data: Data(rustBuffer: buf))
        let value = try read(from: &reader)
        if hasRemaining(reader) {
            throw UniffiInternalError.incompleteData
        }
        buf.deallocate()
        return value
    }

    public static func lower(_ value: SwiftType) -> RustBuffer {
        var writer = createWriter()
        write(value, into: &writer)
        return RustBuffer(bytes: writer)
    }
}

// An error type for FFI errors. These errors occur at the UniFFI level, not
// the library level.
private enum UniffiInternalError: LocalizedError {
    case bufferOverflow
    case incompleteData
    case unexpectedOptionalTag
    case unexpectedEnumCase
    case unexpectedNullPointer
    case unexpectedRustCallStatusCode
    case unexpectedRustCallError
    case unexpectedStaleHandle
    case rustPanic(_ message: String)

    public var errorDescription: String? {
        switch self {
        case .bufferOverflow: "Reading the requested value would read past the end of the buffer"
        case .incompleteData: "The buffer still has data after lifting its containing value"
        case .unexpectedOptionalTag: "Unexpected optional tag; should be 0 or 1"
        case .unexpectedEnumCase: "Raw enum value doesn't match any cases"
        case .unexpectedNullPointer: "Raw pointer value was null"
        case .unexpectedRustCallStatusCode: "Unexpected RustCallStatus code"
        case .unexpectedRustCallError: "CALL_ERROR but no errorClass specified"
        case .unexpectedStaleHandle: "The object in the handle map has been dropped already"
        case let .rustPanic(message): message
        }
    }
}

private extension NSLock {
    func withLock<T>(f: () throws -> T) rethrows -> T {
        lock()
        defer { self.unlock() }
        return try f()
    }
}

private let CALL_SUCCESS: Int8 = 0
private let CALL_ERROR: Int8 = 1
private let CALL_UNEXPECTED_ERROR: Int8 = 2
private let CALL_CANCELLED: Int8 = 3

private extension RustCallStatus {
    init() {
        self.init(
            code: CALL_SUCCESS,
            errorBuf: RustBuffer(
                capacity: 0,
                len: 0,
                data: nil
            )
        )
    }
}

private func rustCall<T>(_ callback: (UnsafeMutablePointer<RustCallStatus>) -> T) throws -> T {
    let neverThrow: ((RustBuffer) throws -> Never)? = nil
    return try makeRustCall(callback, errorHandler: neverThrow)
}

private func rustCallWithError<T>(
    _ errorHandler: @escaping (RustBuffer) throws -> some Swift.Error,
    _ callback: (UnsafeMutablePointer<RustCallStatus>) -> T
) throws -> T {
    try makeRustCall(callback, errorHandler: errorHandler)
}

private func makeRustCall<T>(
    _ callback: (UnsafeMutablePointer<RustCallStatus>) -> T,
    errorHandler: ((RustBuffer) throws -> some Swift.Error)?
) throws -> T {
    uniffiEnsureInitialized()
    var callStatus = RustCallStatus()
    let returnedVal = callback(&callStatus)
    try uniffiCheckCallStatus(callStatus: callStatus, errorHandler: errorHandler)
    return returnedVal
}

private func uniffiCheckCallStatus(
    callStatus: RustCallStatus,
    errorHandler: ((RustBuffer) throws -> some Swift.Error)?
) throws {
    switch callStatus.code {
    case CALL_SUCCESS:
        return

    case CALL_ERROR:
        if let errorHandler {
            throw try errorHandler(callStatus.errorBuf)
        } else {
            callStatus.errorBuf.deallocate()
            throw UniffiInternalError.unexpectedRustCallError
        }

    case CALL_UNEXPECTED_ERROR:
        // When the rust code sees a panic, it tries to construct a RustBuffer
        // with the message.  But if that code panics, then it just sends back
        // an empty buffer.
        if callStatus.errorBuf.len > 0 {
            throw try UniffiInternalError.rustPanic(FfiConverterString.lift(callStatus.errorBuf))
        } else {
            callStatus.errorBuf.deallocate()
            throw UniffiInternalError.rustPanic("Rust panic")
        }

    case CALL_CANCELLED:
        fatalError("Cancellation not supported yet")

    default:
        throw UniffiInternalError.unexpectedRustCallStatusCode
    }
}

private func uniffiTraitInterfaceCall<T>(
    callStatus: UnsafeMutablePointer<RustCallStatus>,
    makeCall: () throws -> T,
    writeReturn: (T) -> Void
) {
    do {
        try writeReturn(makeCall())
    } catch {
        callStatus.pointee.code = CALL_UNEXPECTED_ERROR
        callStatus.pointee.errorBuf = FfiConverterString.lower(String(describing: error))
    }
}

private func uniffiTraitInterfaceCallWithError<T, E>(
    callStatus: UnsafeMutablePointer<RustCallStatus>,
    makeCall: () throws -> T,
    writeReturn: (T) -> Void,
    lowerError: (E) -> RustBuffer
) {
    do {
        try writeReturn(makeCall())
    } catch let error as E {
        callStatus.pointee.code = CALL_ERROR
        callStatus.pointee.errorBuf = lowerError(error)
    } catch {
        callStatus.pointee.code = CALL_UNEXPECTED_ERROR
        callStatus.pointee.errorBuf = FfiConverterString.lower(String(describing: error))
    }
}

private class UniffiHandleMap<T> {
    private var map: [UInt64: T] = [:]
    private let lock = NSLock()
    private var currentHandle: UInt64 = 1

    func insert(obj: T) -> UInt64 {
        lock.withLock {
            let handle = currentHandle
            currentHandle += 1
            map[handle] = obj
            return handle
        }
    }

    func get(handle: UInt64) throws -> T {
        try lock.withLock {
            guard let obj = map[handle] else {
                throw UniffiInternalError.unexpectedStaleHandle
            }
            return obj
        }
    }

    @discardableResult
    func remove(handle: UInt64) throws -> T {
        try lock.withLock {
            guard let obj = map.removeValue(forKey: handle) else {
                throw UniffiInternalError.unexpectedStaleHandle
            }
            return obj
        }
    }

    var count: Int {
        map.count
    }
}

// Public interface members begin here.

private struct FfiConverterUInt16: FfiConverterPrimitive {
    typealias FfiType = UInt16
    typealias SwiftType = UInt16

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> UInt16 {
        try lift(readInt(&buf))
    }

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        writeInt(&buf, lower(value))
    }
}

private struct FfiConverterUInt32: FfiConverterPrimitive {
    typealias FfiType = UInt32
    typealias SwiftType = UInt32

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> UInt32 {
        try lift(readInt(&buf))
    }

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        writeInt(&buf, lower(value))
    }
}

private struct FfiConverterUInt64: FfiConverterPrimitive {
    typealias FfiType = UInt64
    typealias SwiftType = UInt64

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> UInt64 {
        try lift(readInt(&buf))
    }

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        writeInt(&buf, lower(value))
    }
}

private struct FfiConverterDouble: FfiConverterPrimitive {
    typealias FfiType = Double
    typealias SwiftType = Double

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Double {
        try lift(readDouble(&buf))
    }

    public static func write(_ value: Double, into buf: inout [UInt8]) {
        writeDouble(&buf, lower(value))
    }
}

private struct FfiConverterBool: FfiConverter {
    typealias FfiType = Int8
    typealias SwiftType = Bool

    public static func lift(_ value: Int8) throws -> Bool {
        value != 0
    }

    public static func lower(_ value: Bool) -> Int8 {
        value ? 1 : 0
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Bool {
        try lift(readInt(&buf))
    }

    public static func write(_ value: Bool, into buf: inout [UInt8]) {
        writeInt(&buf, lower(value))
    }
}

private struct FfiConverterString: FfiConverter {
    typealias SwiftType = String
    typealias FfiType = RustBuffer

    public static func lift(_ value: RustBuffer) throws -> String {
        defer {
            value.deallocate()
        }
        if value.data == nil {
            return String()
        }
        let bytes = UnsafeBufferPointer<UInt8>(start: value.data!, count: Int(value.len))
        return String(bytes: bytes, encoding: String.Encoding.utf8)!
    }

    public static func lower(_ value: String) -> RustBuffer {
        value.utf8CString.withUnsafeBufferPointer { ptr in
            // The swift string gives us int8_t, we want uint8_t.
            ptr.withMemoryRebound(to: UInt8.self) { ptr in
                // The swift string gives us a trailing null byte, we don't want it.
                let buf = UnsafeBufferPointer(rebasing: ptr.prefix(upTo: ptr.count - 1))
                return RustBuffer.from(buf)
            }
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> String {
        let len: Int32 = try readInt(&buf)
        return try String(bytes: readBytes(&buf, count: Int(len)), encoding: String.Encoding.utf8)!
    }

    public static func write(_ value: String, into buf: inout [UInt8]) {
        let len = Int32(value.utf8.count)
        writeInt(&buf, len)
        writeBytes(&buf, value.utf8)
    }
}

private struct FfiConverterData: FfiConverterRustBuffer {
    typealias SwiftType = Data

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Data {
        let len: Int32 = try readInt(&buf)
        return try Data(readBytes(&buf, count: Int(len)))
    }

    public static func write(_ value: Data, into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        writeBytes(&buf, value)
    }
}

private struct FfiConverterTimestamp: FfiConverterRustBuffer {
    typealias SwiftType = Date

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Date {
        let seconds: Int64 = try readInt(&buf)
        let nanoseconds: UInt32 = try readInt(&buf)
        if seconds >= 0 {
            let delta = Double(seconds) + (Double(nanoseconds) / 1.0e9)
            return Date(timeIntervalSince1970: delta)
        } else {
            let delta = Double(seconds) - (Double(nanoseconds) / 1.0e9)
            return Date(timeIntervalSince1970: delta)
        }
    }

    public static func write(_ value: Date, into buf: inout [UInt8]) {
        var delta = value.timeIntervalSince1970
        var sign: Int64 = 1
        if delta < 0 {
            // The nanoseconds portion of the epoch offset must always be
            // positive, to simplify the calculation we will use the absolute
            // value of the offset.
            sign = -1
            delta = -delta
        }
        if delta.rounded(.down) > Double(Int64.max) {
            fatalError("Timestamp overflow, exceeds max bounds supported by Uniffi")
        }
        let seconds = Int64(delta)
        let nanoseconds = UInt32((delta - Double(seconds)) * 1.0e9)
        writeInt(&buf, sign * seconds)
        writeInt(&buf, nanoseconds)
    }
}

/**
 * Manages the navigation lifecycle through a route,
 * returning an updated state given inputs like user location.
 *
 * Notes for implementing a new platform:
 * - A controller is bound to a single route; if you want recalculation, create a new instance.
 * - This is a pure type (no interior mutability), so a core function of your platform code is responsibly managing mutable state.
 */
public protocol NavigationControllerProtocol: AnyObject {
    /**
     * Advances navigation to the next step.
     *
     * Depending on the advancement strategy, this may be automatic.
     * For other cases, it is desirable to advance to the next step manually (ex: walking in an
     * urban tunnel). We leave this decision to the app developer and provide this as a convenience.
     *
     * This method is takes the intermediate state (e.g. from `update_user_location`) and advances if necessary.
     * As a result, you do not to re-calculate things like deviation or the snapped user location (search this file for usage of this function).
     */
    func advanceToNextStep(state: TripState) -> TripState

    /**
     * Returns initial trip state as if the user had just started the route with no progress.
     */
    func getInitialState(location: UserLocation) -> TripState

    /**
     * Updates the user's current location and updates the navigation state accordingly.
     *
     * # Panics
     *
     * If there is no current step ([`TripState::Navigating`] has an empty `remainingSteps` value),
     * this function will panic.
     */
    func updateUserLocation(location: UserLocation, state: TripState) -> TripState
}

/**
 * Manages the navigation lifecycle through a route,
 * returning an updated state given inputs like user location.
 *
 * Notes for implementing a new platform:
 * - A controller is bound to a single route; if you want recalculation, create a new instance.
 * - This is a pure type (no interior mutability), so a core function of your platform code is responsibly managing mutable state.
 */
open class NavigationController:
    NavigationControllerProtocol
{
    fileprivate let pointer: UnsafeMutableRawPointer!

    /// Used to instantiate a [FFIObject] without an actual pointer, for fakes in tests, mostly.
    public struct NoPointer {
        public init() {}
    }

    // TODO: We'd like this to be `private` but for Swifty reasons,
    // we can't implement `FfiConverter` without making this `required` and we can't
    // make it `required` without making it `public`.
    public required init(unsafeFromRawPointer pointer: UnsafeMutableRawPointer) {
        self.pointer = pointer
    }

    /// This constructor can be used to instantiate a fake object.
    /// - Parameter noPointer: Placeholder value so we can have a constructor separate from the default empty one that
    /// may be implemented for classes extending [FFIObject].
    ///
    /// - Warning:
    ///     Any object instantiated with this constructor cannot be passed to an actual Rust-backed object. Since there
    /// isn't a backing [Pointer] the FFI lower functions will crash.
    public init(noPointer _: NoPointer) {
        pointer = nil
    }

    public func uniffiClonePointer() -> UnsafeMutableRawPointer {
        try! rustCall { uniffi_ferrostar_fn_clone_navigationcontroller(self.pointer, $0) }
    }

    /**
     * Create a navigation controller for a route and configuration.
     */
    public convenience init(route: Route, config: NavigationControllerConfig) {
        let pointer =
            try! rustCall {
                uniffi_ferrostar_fn_constructor_navigationcontroller_new(
                    FfiConverterTypeRoute.lower(route),
                    FfiConverterTypeNavigationControllerConfig.lower(config), $0
                )
            }
        self.init(unsafeFromRawPointer: pointer)
    }

    deinit {
        guard let pointer else {
            return
        }

        try! rustCall { uniffi_ferrostar_fn_free_navigationcontroller(pointer, $0) }
    }

    /**
     * Advances navigation to the next step.
     *
     * Depending on the advancement strategy, this may be automatic.
     * For other cases, it is desirable to advance to the next step manually (ex: walking in an
     * urban tunnel). We leave this decision to the app developer and provide this as a convenience.
     *
     * This method is takes the intermediate state (e.g. from `update_user_location`) and advances if necessary.
     * As a result, you do not to re-calculate things like deviation or the snapped user location (search this file for usage of this function).
     */
    open func advanceToNextStep(state: TripState) -> TripState {
        try! FfiConverterTypeTripState.lift(try! rustCall {
            uniffi_ferrostar_fn_method_navigationcontroller_advance_to_next_step(self.uniffiClonePointer(),
                                                                                 FfiConverterTypeTripState.lower(state),
                                                                                 $0)
        })
    }

    /**
     * Returns initial trip state as if the user had just started the route with no progress.
     */
    open func getInitialState(location: UserLocation) -> TripState {
        try! FfiConverterTypeTripState.lift(try! rustCall {
            uniffi_ferrostar_fn_method_navigationcontroller_get_initial_state(self.uniffiClonePointer(),
                                                                              FfiConverterTypeUserLocation
                                                                                  .lower(location), $0)
        })
    }

    /**
     * Updates the user's current location and updates the navigation state accordingly.
     *
     * # Panics
     *
     * If there is no current step ([`TripState::Navigating`] has an empty `remainingSteps` value),
     * this function will panic.
     */
    open func updateUserLocation(location: UserLocation, state: TripState) -> TripState {
        try! FfiConverterTypeTripState.lift(try! rustCall {
            uniffi_ferrostar_fn_method_navigationcontroller_update_user_location(self.uniffiClonePointer(),
                                                                                 FfiConverterTypeUserLocation
                                                                                     .lower(location),
                                                                                 FfiConverterTypeTripState.lower(state),
                                                                                 $0)
        })
    }
}

public struct FfiConverterTypeNavigationController: FfiConverter {
    typealias FfiType = UnsafeMutableRawPointer
    typealias SwiftType = NavigationController

    public static func lift(_ pointer: UnsafeMutableRawPointer) throws -> NavigationController {
        NavigationController(unsafeFromRawPointer: pointer)
    }

    public static func lower(_ value: NavigationController) -> UnsafeMutableRawPointer {
        value.uniffiClonePointer()
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> NavigationController {
        let v: UInt64 = try readInt(&buf)
        // The Rust code won't compile if a pointer won't fit in a UInt64.
        // We have to go via `UInt` because that's the thing that's the size of a pointer.
        let ptr = UnsafeMutableRawPointer(bitPattern: UInt(truncatingIfNeeded: v))
        if ptr == nil {
            throw UniffiInternalError.unexpectedNullPointer
        }
        return try lift(ptr!)
    }

    public static func write(_ value: NavigationController, into buf: inout [UInt8]) {
        // This fiddling is because `Int` is the thing that's the same size as a pointer.
        // The Rust code won't compile if a pointer won't fit in a `UInt64`.
        writeInt(&buf, UInt64(bitPattern: Int64(Int(bitPattern: lower(value)))))
    }
}

public func FfiConverterTypeNavigationController_lift(_ pointer: UnsafeMutableRawPointer) throws
    -> NavigationController
{
    try FfiConverterTypeNavigationController.lift(pointer)
}

public func FfiConverterTypeNavigationController_lower(_ value: NavigationController) -> UnsafeMutableRawPointer {
    FfiConverterTypeNavigationController.lower(value)
}

/**
 * The route adapter bridges between the common core and a routing backend where interaction takes place
 * over a generic request/response flow (typically over a network;
 * local/offline routers **do not use this object** as the interaction patterns are different).
 *
 * This is essentially the composite of the [`RouteRequestGenerator`] and [`RouteResponseParser`]
 * traits, but it provides one further level of abstraction which is helpful to consumers.
 * As there is no way to signal compatibility between request generators and response parsers,
 * the [`RouteAdapter`] provides convenience constructors which take the guesswork out of it,
 * while still leaving consumers free to implement one or both halves.
 *
 * In the future, we may provide additional methods or conveniences, and this
 * indirection leaves the design open to such changes without necessarily breaking source
 * compatibility.
 * One such possible extension would be the ability to fetch more detailed attributes in real time.
 * This is supported by the Valhalla stack, among others.
 *
 * Ideas  welcome re: how to signal compatibility between request generators and response parsers.
 * I don't think we can do this in the type system, since one of the reasons for the split design
 * is modularity, including the possibility of user-provided implementations, and these will not
 * always be of a "known" type to the Rust side.
 */
public protocol RouteAdapterProtocol: AnyObject {
    func generateRequest(userLocation: UserLocation, waypoints: [Waypoint]) throws -> RouteRequest

    func parseResponse(response: Data) throws -> [Route]
}

/**
 * The route adapter bridges between the common core and a routing backend where interaction takes place
 * over a generic request/response flow (typically over a network;
 * local/offline routers **do not use this object** as the interaction patterns are different).
 *
 * This is essentially the composite of the [`RouteRequestGenerator`] and [`RouteResponseParser`]
 * traits, but it provides one further level of abstraction which is helpful to consumers.
 * As there is no way to signal compatibility between request generators and response parsers,
 * the [`RouteAdapter`] provides convenience constructors which take the guesswork out of it,
 * while still leaving consumers free to implement one or both halves.
 *
 * In the future, we may provide additional methods or conveniences, and this
 * indirection leaves the design open to such changes without necessarily breaking source
 * compatibility.
 * One such possible extension would be the ability to fetch more detailed attributes in real time.
 * This is supported by the Valhalla stack, among others.
 *
 * Ideas  welcome re: how to signal compatibility between request generators and response parsers.
 * I don't think we can do this in the type system, since one of the reasons for the split design
 * is modularity, including the possibility of user-provided implementations, and these will not
 * always be of a "known" type to the Rust side.
 */
open class RouteAdapter:
    RouteAdapterProtocol
{
    fileprivate let pointer: UnsafeMutableRawPointer!

    /// Used to instantiate a [FFIObject] without an actual pointer, for fakes in tests, mostly.
    public struct NoPointer {
        public init() {}
    }

    // TODO: We'd like this to be `private` but for Swifty reasons,
    // we can't implement `FfiConverter` without making this `required` and we can't
    // make it `required` without making it `public`.
    public required init(unsafeFromRawPointer pointer: UnsafeMutableRawPointer) {
        self.pointer = pointer
    }

    /// This constructor can be used to instantiate a fake object.
    /// - Parameter noPointer: Placeholder value so we can have a constructor separate from the default empty one that
    /// may be implemented for classes extending [FFIObject].
    ///
    /// - Warning:
    ///     Any object instantiated with this constructor cannot be passed to an actual Rust-backed object. Since there
    /// isn't a backing [Pointer] the FFI lower functions will crash.
    public init(noPointer _: NoPointer) {
        pointer = nil
    }

    public func uniffiClonePointer() -> UnsafeMutableRawPointer {
        try! rustCall { uniffi_ferrostar_fn_clone_routeadapter(self.pointer, $0) }
    }

    public convenience init(requestGenerator: RouteRequestGenerator, responseParser: RouteResponseParser) {
        let pointer =
            try! rustCall {
                uniffi_ferrostar_fn_constructor_routeadapter_new(
                    FfiConverterTypeRouteRequestGenerator.lower(requestGenerator),
                    FfiConverterTypeRouteResponseParser.lower(responseParser), $0
                )
            }
        self.init(unsafeFromRawPointer: pointer)
    }

    deinit {
        guard let pointer else {
            return
        }

        try! rustCall { uniffi_ferrostar_fn_free_routeadapter(pointer, $0) }
    }

    public static func newValhallaHttp(endpointUrl: String, profile: String,
                                       optionsJson: String?) throws -> RouteAdapter
    {
        try FfiConverterTypeRouteAdapter.lift(rustCallWithError(FfiConverterTypeInstantiationError.lift) {
            uniffi_ferrostar_fn_constructor_routeadapter_new_valhalla_http(
                FfiConverterString.lower(endpointUrl),
                FfiConverterString.lower(profile),
                FfiConverterOptionString.lower(optionsJson), $0
            )
        })
    }

    open func generateRequest(userLocation: UserLocation, waypoints: [Waypoint]) throws -> RouteRequest {
        try FfiConverterTypeRouteRequest.lift(rustCallWithError(FfiConverterTypeRoutingRequestGenerationError.lift) {
            uniffi_ferrostar_fn_method_routeadapter_generate_request(self.uniffiClonePointer(),
                                                                     FfiConverterTypeUserLocation.lower(userLocation),
                                                                     FfiConverterSequenceTypeWaypoint.lower(waypoints),
                                                                     $0)
        })
    }

    open func parseResponse(response: Data) throws -> [Route] {
        try FfiConverterSequenceTypeRoute.lift(rustCallWithError(FfiConverterTypeParsingError.lift) {
            uniffi_ferrostar_fn_method_routeadapter_parse_response(self.uniffiClonePointer(),
                                                                   FfiConverterData.lower(response), $0)
        })
    }
}

public struct FfiConverterTypeRouteAdapter: FfiConverter {
    typealias FfiType = UnsafeMutableRawPointer
    typealias SwiftType = RouteAdapter

    public static func lift(_ pointer: UnsafeMutableRawPointer) throws -> RouteAdapter {
        RouteAdapter(unsafeFromRawPointer: pointer)
    }

    public static func lower(_ value: RouteAdapter) -> UnsafeMutableRawPointer {
        value.uniffiClonePointer()
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RouteAdapter {
        let v: UInt64 = try readInt(&buf)
        // The Rust code won't compile if a pointer won't fit in a UInt64.
        // We have to go via `UInt` because that's the thing that's the size of a pointer.
        let ptr = UnsafeMutableRawPointer(bitPattern: UInt(truncatingIfNeeded: v))
        if ptr == nil {
            throw UniffiInternalError.unexpectedNullPointer
        }
        return try lift(ptr!)
    }

    public static func write(_ value: RouteAdapter, into buf: inout [UInt8]) {
        // This fiddling is because `Int` is the thing that's the same size as a pointer.
        // The Rust code won't compile if a pointer won't fit in a `UInt64`.
        writeInt(&buf, UInt64(bitPattern: Int64(Int(bitPattern: lower(value)))))
    }
}

public func FfiConverterTypeRouteAdapter_lift(_ pointer: UnsafeMutableRawPointer) throws -> RouteAdapter {
    try FfiConverterTypeRouteAdapter.lift(pointer)
}

public func FfiConverterTypeRouteAdapter_lower(_ value: RouteAdapter) -> UnsafeMutableRawPointer {
    FfiConverterTypeRouteAdapter.lower(value)
}

/**
 * A custom deviation detector (for extending the behavior of [`RouteDeviationTracking`]).
 *
 * This allows for arbitrarily complex implementations when the provided ones are not enough.
 * For example, detecting that the user is proceeding the wrong direction by keeping a ring buffer
 * of recent locations, or perform local map matching.
 */
public protocol RouteDeviationDetector: AnyObject {
    /**
     * Determines whether the user is following the route correctly or not.
     *
     * NOTE: This function has a single responsibility.
     * Side-effects like whether to recalculate a route are left to higher levels,
     * and implementations should only be concerned with determining the facts.
     */
    func checkRouteDeviation(location: UserLocation, route: Route, currentRouteStep: RouteStep) -> RouteDeviation
}

/**
 * A custom deviation detector (for extending the behavior of [`RouteDeviationTracking`]).
 *
 * This allows for arbitrarily complex implementations when the provided ones are not enough.
 * For example, detecting that the user is proceeding the wrong direction by keeping a ring buffer
 * of recent locations, or perform local map matching.
 */
open class RouteDeviationDetectorImpl:
    RouteDeviationDetector
{
    fileprivate let pointer: UnsafeMutableRawPointer!

    /// Used to instantiate a [FFIObject] without an actual pointer, for fakes in tests, mostly.
    public struct NoPointer {
        public init() {}
    }

    // TODO: We'd like this to be `private` but for Swifty reasons,
    // we can't implement `FfiConverter` without making this `required` and we can't
    // make it `required` without making it `public`.
    public required init(unsafeFromRawPointer pointer: UnsafeMutableRawPointer) {
        self.pointer = pointer
    }

    /// This constructor can be used to instantiate a fake object.
    /// - Parameter noPointer: Placeholder value so we can have a constructor separate from the default empty one that
    /// may be implemented for classes extending [FFIObject].
    ///
    /// - Warning:
    ///     Any object instantiated with this constructor cannot be passed to an actual Rust-backed object. Since there
    /// isn't a backing [Pointer] the FFI lower functions will crash.
    public init(noPointer _: NoPointer) {
        pointer = nil
    }

    public func uniffiClonePointer() -> UnsafeMutableRawPointer {
        try! rustCall { uniffi_ferrostar_fn_clone_routedeviationdetector(self.pointer, $0) }
    }

    // No primary constructor declared for this class.

    deinit {
        guard let pointer else {
            return
        }

        try! rustCall { uniffi_ferrostar_fn_free_routedeviationdetector(pointer, $0) }
    }

    /**
     * Determines whether the user is following the route correctly or not.
     *
     * NOTE: This function has a single responsibility.
     * Side-effects like whether to recalculate a route are left to higher levels,
     * and implementations should only be concerned with determining the facts.
     */
    open func checkRouteDeviation(location: UserLocation, route: Route, currentRouteStep: RouteStep) -> RouteDeviation {
        try! FfiConverterTypeRouteDeviation.lift(try! rustCall {
            uniffi_ferrostar_fn_method_routedeviationdetector_check_route_deviation(self.uniffiClonePointer(),
                                                                                    FfiConverterTypeUserLocation.lower(
                                                                                        location
                                                                                    ),
                                                                                    FfiConverterTypeRoute.lower(route),
                                                                                    FfiConverterTypeRouteStep.lower(
                                                                                        currentRouteStep
                                                                                    ),
                                                                                    $0)
        })
    }
}

// Magic number for the Rust proxy to call using the same mechanism as every other method,
// to free the callback once it's dropped by Rust.
private let IDX_CALLBACK_FREE: Int32 = 0
// Callback return codes
private let UNIFFI_CALLBACK_SUCCESS: Int32 = 0
private let UNIFFI_CALLBACK_ERROR: Int32 = 1
private let UNIFFI_CALLBACK_UNEXPECTED_ERROR: Int32 = 2

// Put the implementation in a struct so we don't pollute the top-level namespace
private enum UniffiCallbackInterfaceRouteDeviationDetector {
    // Create the VTable using a series of closures.
    // Swift automatically converts these into C callback functions.
    static var vtable: UniffiVTableCallbackInterfaceRouteDeviationDetector = .init(
        checkRouteDeviation: { (
            uniffiHandle: UInt64,
            location: RustBuffer,
            route: RustBuffer,
            currentRouteStep: RustBuffer,
            uniffiOutReturn: UnsafeMutablePointer<RustBuffer>,
            uniffiCallStatus: UnsafeMutablePointer<RustCallStatus>
        ) in
            let makeCall = {
                () throws -> RouteDeviation in
                guard let uniffiObj = try? FfiConverterTypeRouteDeviationDetector.handleMap.get(handle: uniffiHandle)
                else {
                    throw UniffiInternalError.unexpectedStaleHandle
                }
                return try uniffiObj.checkRouteDeviation(
                    location: FfiConverterTypeUserLocation.lift(location),
                    route: FfiConverterTypeRoute.lift(route),
                    currentRouteStep: FfiConverterTypeRouteStep.lift(currentRouteStep)
                )
            }

            let writeReturn = { uniffiOutReturn.pointee = FfiConverterTypeRouteDeviation.lower($0) }
            uniffiTraitInterfaceCall(
                callStatus: uniffiCallStatus,
                makeCall: makeCall,
                writeReturn: writeReturn
            )
        },
        uniffiFree: { (uniffiHandle: UInt64) in
            let result = try? FfiConverterTypeRouteDeviationDetector.handleMap.remove(handle: uniffiHandle)
            if result == nil {
                print("Uniffi callback interface RouteDeviationDetector: handle missing in uniffiFree")
            }
        }
    )
}

private func uniffiCallbackInitRouteDeviationDetector() {
    uniffi_ferrostar_fn_init_callback_vtable_routedeviationdetector(&UniffiCallbackInterfaceRouteDeviationDetector
        .vtable)
}

public struct FfiConverterTypeRouteDeviationDetector: FfiConverter {
    fileprivate static var handleMap = UniffiHandleMap<RouteDeviationDetector>()

    typealias FfiType = UnsafeMutableRawPointer
    typealias SwiftType = RouteDeviationDetector

    public static func lift(_ pointer: UnsafeMutableRawPointer) throws -> RouteDeviationDetector {
        RouteDeviationDetectorImpl(unsafeFromRawPointer: pointer)
    }

    public static func lower(_ value: RouteDeviationDetector) -> UnsafeMutableRawPointer {
        guard let ptr = UnsafeMutableRawPointer(bitPattern: UInt(truncatingIfNeeded: handleMap.insert(obj: value)))
        else {
            fatalError("Cast to UnsafeMutableRawPointer failed")
        }
        return ptr
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RouteDeviationDetector {
        let v: UInt64 = try readInt(&buf)
        // The Rust code won't compile if a pointer won't fit in a UInt64.
        // We have to go via `UInt` because that's the thing that's the size of a pointer.
        let ptr = UnsafeMutableRawPointer(bitPattern: UInt(truncatingIfNeeded: v))
        if ptr == nil {
            throw UniffiInternalError.unexpectedNullPointer
        }
        return try lift(ptr!)
    }

    public static func write(_ value: RouteDeviationDetector, into buf: inout [UInt8]) {
        // This fiddling is because `Int` is the thing that's the same size as a pointer.
        // The Rust code won't compile if a pointer won't fit in a `UInt64`.
        writeInt(&buf, UInt64(bitPattern: Int64(Int(bitPattern: lower(value)))))
    }
}

public func FfiConverterTypeRouteDeviationDetector_lift(_ pointer: UnsafeMutableRawPointer) throws
    -> RouteDeviationDetector
{
    try FfiConverterTypeRouteDeviationDetector.lift(pointer)
}

public func FfiConverterTypeRouteDeviationDetector_lower(_ value: RouteDeviationDetector) -> UnsafeMutableRawPointer {
    FfiConverterTypeRouteDeviationDetector.lower(value)
}

/**
 * A trait describing any object capable of generating [`RouteRequest`]s.
 *
 * The interface is intentionally generic. Every routing backend has its own set of
 * parameters, including a "profile," max travel speed, units of speed and distance, and more.
 * It is assumed that these properties will be set at construction time or otherwise configured
 * before use, so that we can keep the public interface as generic as possible.
 *
 * Implementations may be either in Rust (most popular engines should eventually have Rust
 * glue code) or foreign code.
 */
public protocol RouteRequestGenerator: AnyObject {
    /**
     * Generates a routing backend request given the set of locations.
     *
     * While most implementations will treat the locations as an ordered sequence, this is not
     * guaranteed (ex: an optimized router).
     */
    func generateRequest(userLocation: UserLocation, waypoints: [Waypoint]) throws -> RouteRequest
}

/**
 * A trait describing any object capable of generating [`RouteRequest`]s.
 *
 * The interface is intentionally generic. Every routing backend has its own set of
 * parameters, including a "profile," max travel speed, units of speed and distance, and more.
 * It is assumed that these properties will be set at construction time or otherwise configured
 * before use, so that we can keep the public interface as generic as possible.
 *
 * Implementations may be either in Rust (most popular engines should eventually have Rust
 * glue code) or foreign code.
 */
open class RouteRequestGeneratorImpl:
    RouteRequestGenerator
{
    fileprivate let pointer: UnsafeMutableRawPointer!

    /// Used to instantiate a [FFIObject] without an actual pointer, for fakes in tests, mostly.
    public struct NoPointer {
        public init() {}
    }

    // TODO: We'd like this to be `private` but for Swifty reasons,
    // we can't implement `FfiConverter` without making this `required` and we can't
    // make it `required` without making it `public`.
    public required init(unsafeFromRawPointer pointer: UnsafeMutableRawPointer) {
        self.pointer = pointer
    }

    /// This constructor can be used to instantiate a fake object.
    /// - Parameter noPointer: Placeholder value so we can have a constructor separate from the default empty one that
    /// may be implemented for classes extending [FFIObject].
    ///
    /// - Warning:
    ///     Any object instantiated with this constructor cannot be passed to an actual Rust-backed object. Since there
    /// isn't a backing [Pointer] the FFI lower functions will crash.
    public init(noPointer _: NoPointer) {
        pointer = nil
    }

    public func uniffiClonePointer() -> UnsafeMutableRawPointer {
        try! rustCall { uniffi_ferrostar_fn_clone_routerequestgenerator(self.pointer, $0) }
    }

    // No primary constructor declared for this class.

    deinit {
        guard let pointer else {
            return
        }

        try! rustCall { uniffi_ferrostar_fn_free_routerequestgenerator(pointer, $0) }
    }

    /**
     * Generates a routing backend request given the set of locations.
     *
     * While most implementations will treat the locations as an ordered sequence, this is not
     * guaranteed (ex: an optimized router).
     */
    open func generateRequest(userLocation: UserLocation, waypoints: [Waypoint]) throws -> RouteRequest {
        try FfiConverterTypeRouteRequest.lift(rustCallWithError(FfiConverterTypeRoutingRequestGenerationError.lift) {
            uniffi_ferrostar_fn_method_routerequestgenerator_generate_request(self.uniffiClonePointer(),
                                                                              FfiConverterTypeUserLocation
                                                                                  .lower(userLocation),
                                                                              FfiConverterSequenceTypeWaypoint
                                                                                  .lower(waypoints),
                                                                              $0)
        })
    }
}

// Put the implementation in a struct so we don't pollute the top-level namespace
private enum UniffiCallbackInterfaceRouteRequestGenerator {
    // Create the VTable using a series of closures.
    // Swift automatically converts these into C callback functions.
    static var vtable: UniffiVTableCallbackInterfaceRouteRequestGenerator = .init(
        generateRequest: { (
            uniffiHandle: UInt64,
            userLocation: RustBuffer,
            waypoints: RustBuffer,
            uniffiOutReturn: UnsafeMutablePointer<RustBuffer>,
            uniffiCallStatus: UnsafeMutablePointer<RustCallStatus>
        ) in
            let makeCall = {
                () throws -> RouteRequest in
                guard let uniffiObj = try? FfiConverterTypeRouteRequestGenerator.handleMap.get(handle: uniffiHandle)
                else {
                    throw UniffiInternalError.unexpectedStaleHandle
                }
                return try uniffiObj.generateRequest(
                    userLocation: FfiConverterTypeUserLocation.lift(userLocation),
                    waypoints: FfiConverterSequenceTypeWaypoint.lift(waypoints)
                )
            }

            let writeReturn = { uniffiOutReturn.pointee = FfiConverterTypeRouteRequest.lower($0) }
            uniffiTraitInterfaceCallWithError(
                callStatus: uniffiCallStatus,
                makeCall: makeCall,
                writeReturn: writeReturn,
                lowerError: FfiConverterTypeRoutingRequestGenerationError.lower
            )
        },
        uniffiFree: { (uniffiHandle: UInt64) in
            let result = try? FfiConverterTypeRouteRequestGenerator.handleMap.remove(handle: uniffiHandle)
            if result == nil {
                print("Uniffi callback interface RouteRequestGenerator: handle missing in uniffiFree")
            }
        }
    )
}

private func uniffiCallbackInitRouteRequestGenerator() {
    uniffi_ferrostar_fn_init_callback_vtable_routerequestgenerator(&UniffiCallbackInterfaceRouteRequestGenerator.vtable)
}

public struct FfiConverterTypeRouteRequestGenerator: FfiConverter {
    fileprivate static var handleMap = UniffiHandleMap<RouteRequestGenerator>()

    typealias FfiType = UnsafeMutableRawPointer
    typealias SwiftType = RouteRequestGenerator

    public static func lift(_ pointer: UnsafeMutableRawPointer) throws -> RouteRequestGenerator {
        RouteRequestGeneratorImpl(unsafeFromRawPointer: pointer)
    }

    public static func lower(_ value: RouteRequestGenerator) -> UnsafeMutableRawPointer {
        guard let ptr = UnsafeMutableRawPointer(bitPattern: UInt(truncatingIfNeeded: handleMap.insert(obj: value)))
        else {
            fatalError("Cast to UnsafeMutableRawPointer failed")
        }
        return ptr
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RouteRequestGenerator {
        let v: UInt64 = try readInt(&buf)
        // The Rust code won't compile if a pointer won't fit in a UInt64.
        // We have to go via `UInt` because that's the thing that's the size of a pointer.
        let ptr = UnsafeMutableRawPointer(bitPattern: UInt(truncatingIfNeeded: v))
        if ptr == nil {
            throw UniffiInternalError.unexpectedNullPointer
        }
        return try lift(ptr!)
    }

    public static func write(_ value: RouteRequestGenerator, into buf: inout [UInt8]) {
        // This fiddling is because `Int` is the thing that's the same size as a pointer.
        // The Rust code won't compile if a pointer won't fit in a `UInt64`.
        writeInt(&buf, UInt64(bitPattern: Int64(Int(bitPattern: lower(value)))))
    }
}

public func FfiConverterTypeRouteRequestGenerator_lift(_ pointer: UnsafeMutableRawPointer) throws
    -> RouteRequestGenerator
{
    try FfiConverterTypeRouteRequestGenerator.lift(pointer)
}

public func FfiConverterTypeRouteRequestGenerator_lower(_ value: RouteRequestGenerator) -> UnsafeMutableRawPointer {
    FfiConverterTypeRouteRequestGenerator.lower(value)
}

/**
 * A generic interface describing any object capable of parsing a response from a routing
 * backend into one or more [`Route`]s.
 */
public protocol RouteResponseParser: AnyObject {
    /**
     * Parses a raw response from the routing backend into a route.
     *
     * We use a sequence of octets as a common interchange format.
     * as this works for all currently conceivable formats (JSON, PBF, etc.).
     */
    func parseResponse(response: Data) throws -> [Route]
}

/**
 * A generic interface describing any object capable of parsing a response from a routing
 * backend into one or more [`Route`]s.
 */
open class RouteResponseParserImpl:
    RouteResponseParser
{
    fileprivate let pointer: UnsafeMutableRawPointer!

    /// Used to instantiate a [FFIObject] without an actual pointer, for fakes in tests, mostly.
    public struct NoPointer {
        public init() {}
    }

    // TODO: We'd like this to be `private` but for Swifty reasons,
    // we can't implement `FfiConverter` without making this `required` and we can't
    // make it `required` without making it `public`.
    public required init(unsafeFromRawPointer pointer: UnsafeMutableRawPointer) {
        self.pointer = pointer
    }

    /// This constructor can be used to instantiate a fake object.
    /// - Parameter noPointer: Placeholder value so we can have a constructor separate from the default empty one that
    /// may be implemented for classes extending [FFIObject].
    ///
    /// - Warning:
    ///     Any object instantiated with this constructor cannot be passed to an actual Rust-backed object. Since there
    /// isn't a backing [Pointer] the FFI lower functions will crash.
    public init(noPointer _: NoPointer) {
        pointer = nil
    }

    public func uniffiClonePointer() -> UnsafeMutableRawPointer {
        try! rustCall { uniffi_ferrostar_fn_clone_routeresponseparser(self.pointer, $0) }
    }

    // No primary constructor declared for this class.

    deinit {
        guard let pointer else {
            return
        }

        try! rustCall { uniffi_ferrostar_fn_free_routeresponseparser(pointer, $0) }
    }

    /**
     * Parses a raw response from the routing backend into a route.
     *
     * We use a sequence of octets as a common interchange format.
     * as this works for all currently conceivable formats (JSON, PBF, etc.).
     */
    open func parseResponse(response: Data) throws -> [Route] {
        try FfiConverterSequenceTypeRoute.lift(rustCallWithError(FfiConverterTypeParsingError.lift) {
            uniffi_ferrostar_fn_method_routeresponseparser_parse_response(self.uniffiClonePointer(),
                                                                          FfiConverterData.lower(response), $0)
        })
    }
}

// Put the implementation in a struct so we don't pollute the top-level namespace
private enum UniffiCallbackInterfaceRouteResponseParser {
    // Create the VTable using a series of closures.
    // Swift automatically converts these into C callback functions.
    static var vtable: UniffiVTableCallbackInterfaceRouteResponseParser = .init(
        parseResponse: { (
            uniffiHandle: UInt64,
            response: RustBuffer,
            uniffiOutReturn: UnsafeMutablePointer<RustBuffer>,
            uniffiCallStatus: UnsafeMutablePointer<RustCallStatus>
        ) in
            let makeCall = {
                () throws -> [Route] in
                guard let uniffiObj = try? FfiConverterTypeRouteResponseParser.handleMap.get(handle: uniffiHandle)
                else {
                    throw UniffiInternalError.unexpectedStaleHandle
                }
                return try uniffiObj.parseResponse(
                    response: FfiConverterData.lift(response)
                )
            }

            let writeReturn = { uniffiOutReturn.pointee = FfiConverterSequenceTypeRoute.lower($0) }
            uniffiTraitInterfaceCallWithError(
                callStatus: uniffiCallStatus,
                makeCall: makeCall,
                writeReturn: writeReturn,
                lowerError: FfiConverterTypeParsingError.lower
            )
        },
        uniffiFree: { (uniffiHandle: UInt64) in
            let result = try? FfiConverterTypeRouteResponseParser.handleMap.remove(handle: uniffiHandle)
            if result == nil {
                print("Uniffi callback interface RouteResponseParser: handle missing in uniffiFree")
            }
        }
    )
}

private func uniffiCallbackInitRouteResponseParser() {
    uniffi_ferrostar_fn_init_callback_vtable_routeresponseparser(&UniffiCallbackInterfaceRouteResponseParser.vtable)
}

public struct FfiConverterTypeRouteResponseParser: FfiConverter {
    fileprivate static var handleMap = UniffiHandleMap<RouteResponseParser>()

    typealias FfiType = UnsafeMutableRawPointer
    typealias SwiftType = RouteResponseParser

    public static func lift(_ pointer: UnsafeMutableRawPointer) throws -> RouteResponseParser {
        RouteResponseParserImpl(unsafeFromRawPointer: pointer)
    }

    public static func lower(_ value: RouteResponseParser) -> UnsafeMutableRawPointer {
        guard let ptr = UnsafeMutableRawPointer(bitPattern: UInt(truncatingIfNeeded: handleMap.insert(obj: value)))
        else {
            fatalError("Cast to UnsafeMutableRawPointer failed")
        }
        return ptr
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RouteResponseParser {
        let v: UInt64 = try readInt(&buf)
        // The Rust code won't compile if a pointer won't fit in a UInt64.
        // We have to go via `UInt` because that's the thing that's the size of a pointer.
        let ptr = UnsafeMutableRawPointer(bitPattern: UInt(truncatingIfNeeded: v))
        if ptr == nil {
            throw UniffiInternalError.unexpectedNullPointer
        }
        return try lift(ptr!)
    }

    public static func write(_ value: RouteResponseParser, into buf: inout [UInt8]) {
        // This fiddling is because `Int` is the thing that's the same size as a pointer.
        // The Rust code won't compile if a pointer won't fit in a `UInt64`.
        writeInt(&buf, UInt64(bitPattern: Int64(Int(bitPattern: lower(value)))))
    }
}

public func FfiConverterTypeRouteResponseParser_lift(_ pointer: UnsafeMutableRawPointer) throws -> RouteResponseParser {
    try FfiConverterTypeRouteResponseParser.lift(pointer)
}

public func FfiConverterTypeRouteResponseParser_lower(_ value: RouteResponseParser) -> UnsafeMutableRawPointer {
    FfiConverterTypeRouteResponseParser.lower(value)
}

/**
 * A geographic bounding box defined by its corners.
 */
public struct BoundingBox {
    /**
     * The southwest corner of the bounding box.
     */
    public var sw: GeographicCoordinate
    /**
     * The northeast corner of the bounding box.
     */
    public var ne: GeographicCoordinate

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The southwest corner of the bounding box.
         */ sw: GeographicCoordinate,
        /**
            * The northeast corner of the bounding box.
            */ ne: GeographicCoordinate
    ) {
        self.sw = sw
        self.ne = ne
    }
}

extension BoundingBox: Equatable, Hashable {
    public static func == (lhs: BoundingBox, rhs: BoundingBox) -> Bool {
        if lhs.sw != rhs.sw {
            return false
        }
        if lhs.ne != rhs.ne {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(sw)
        hasher.combine(ne)
    }
}

public struct FfiConverterTypeBoundingBox: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> BoundingBox {
        try BoundingBox(
            sw: FfiConverterTypeGeographicCoordinate.read(from: &buf),
            ne: FfiConverterTypeGeographicCoordinate.read(from: &buf)
        )
    }

    public static func write(_ value: BoundingBox, into buf: inout [UInt8]) {
        FfiConverterTypeGeographicCoordinate.write(value.sw, into: &buf)
        FfiConverterTypeGeographicCoordinate.write(value.ne, into: &buf)
    }
}

public func FfiConverterTypeBoundingBox_lift(_ buf: RustBuffer) throws -> BoundingBox {
    try FfiConverterTypeBoundingBox.lift(buf)
}

public func FfiConverterTypeBoundingBox_lower(_ value: BoundingBox) -> RustBuffer {
    FfiConverterTypeBoundingBox.lower(value)
}

/**
 * The direction in which the user/device is observed to be traveling.
 */
public struct CourseOverGround {
    /**
     * The direction in which the user's device is traveling, measured in clockwise degrees from
     * true north (N = 0, E = 90, S = 180, W = 270).
     */
    public var degrees: UInt16
    /**
     * The accuracy of the course value, measured in degrees.
     */
    public var accuracy: UInt16?

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The direction in which the user's device is traveling, measured in clockwise degrees from
         * true north (N = 0, E = 90, S = 180, W = 270).
         */ degrees: UInt16,
        /**
            * The accuracy of the course value, measured in degrees.
            */ accuracy: UInt16?
    ) {
        self.degrees = degrees
        self.accuracy = accuracy
    }
}

extension CourseOverGround: Equatable, Hashable {
    public static func == (lhs: CourseOverGround, rhs: CourseOverGround) -> Bool {
        if lhs.degrees != rhs.degrees {
            return false
        }
        if lhs.accuracy != rhs.accuracy {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(degrees)
        hasher.combine(accuracy)
    }
}

public struct FfiConverterTypeCourseOverGround: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> CourseOverGround {
        try CourseOverGround(
            degrees: FfiConverterUInt16.read(from: &buf),
            accuracy: FfiConverterOptionUInt16.read(from: &buf)
        )
    }

    public static func write(_ value: CourseOverGround, into buf: inout [UInt8]) {
        FfiConverterUInt16.write(value.degrees, into: &buf)
        FfiConverterOptionUInt16.write(value.accuracy, into: &buf)
    }
}

public func FfiConverterTypeCourseOverGround_lift(_ buf: RustBuffer) throws -> CourseOverGround {
    try FfiConverterTypeCourseOverGround.lift(buf)
}

public func FfiConverterTypeCourseOverGround_lower(_ value: CourseOverGround) -> RustBuffer {
    FfiConverterTypeCourseOverGround.lower(value)
}

/**
 * A geographic coordinate in WGS84.
 */
public struct GeographicCoordinate {
    /**
     * The latitude (in degrees).
     */
    public var lat: Double
    /**
     * The Longitude (in degrees).
     */
    public var lng: Double

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The latitude (in degrees).
         */ lat: Double,
        /**
            * The Longitude (in degrees).
            */ lng: Double
    ) {
        self.lat = lat
        self.lng = lng
    }
}

extension GeographicCoordinate: Equatable, Hashable {
    public static func == (lhs: GeographicCoordinate, rhs: GeographicCoordinate) -> Bool {
        if lhs.lat != rhs.lat {
            return false
        }
        if lhs.lng != rhs.lng {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(lat)
        hasher.combine(lng)
    }
}

public struct FfiConverterTypeGeographicCoordinate: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> GeographicCoordinate {
        try GeographicCoordinate(
            lat: FfiConverterDouble.read(from: &buf),
            lng: FfiConverterDouble.read(from: &buf)
        )
    }

    public static func write(_ value: GeographicCoordinate, into buf: inout [UInt8]) {
        FfiConverterDouble.write(value.lat, into: &buf)
        FfiConverterDouble.write(value.lng, into: &buf)
    }
}

public func FfiConverterTypeGeographicCoordinate_lift(_ buf: RustBuffer) throws -> GeographicCoordinate {
    try FfiConverterTypeGeographicCoordinate.lift(buf)
}

public func FfiConverterTypeGeographicCoordinate_lower(_ value: GeographicCoordinate) -> RustBuffer {
    FfiConverterTypeGeographicCoordinate.lower(value)
}

/**
 * The heading of the user/device.
 */
public struct Heading {
    /**
     * The heading in degrees relative to true north.
     */
    public var trueHeading: UInt16
    /**
     * The platform specific accuracy of the heading value.
     */
    public var accuracy: UInt16
    /**
     * The time at which the heading was recorded.
     */
    public var timestamp: Date

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The heading in degrees relative to true north.
         */ trueHeading: UInt16,
        /**
            * The platform specific accuracy of the heading value.
            */ accuracy: UInt16,
        /**
            * The time at which the heading was recorded.
            */ timestamp: Date
    ) {
        self.trueHeading = trueHeading
        self.accuracy = accuracy
        self.timestamp = timestamp
    }
}

extension Heading: Equatable, Hashable {
    public static func == (lhs: Heading, rhs: Heading) -> Bool {
        if lhs.trueHeading != rhs.trueHeading {
            return false
        }
        if lhs.accuracy != rhs.accuracy {
            return false
        }
        if lhs.timestamp != rhs.timestamp {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(trueHeading)
        hasher.combine(accuracy)
        hasher.combine(timestamp)
    }
}

public struct FfiConverterTypeHeading: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Heading {
        try Heading(
            trueHeading: FfiConverterUInt16.read(from: &buf),
            accuracy: FfiConverterUInt16.read(from: &buf),
            timestamp: FfiConverterTimestamp.read(from: &buf)
        )
    }

    public static func write(_ value: Heading, into buf: inout [UInt8]) {
        FfiConverterUInt16.write(value.trueHeading, into: &buf)
        FfiConverterUInt16.write(value.accuracy, into: &buf)
        FfiConverterTimestamp.write(value.timestamp, into: &buf)
    }
}

public func FfiConverterTypeHeading_lift(_ buf: RustBuffer) throws -> Heading {
    try FfiConverterTypeHeading.lift(buf)
}

public func FfiConverterTypeHeading_lower(_ value: Heading) -> RustBuffer {
    FfiConverterTypeHeading.lower(value)
}

/**
 * The content of a visual instruction.
 */
public struct LaneInfo {
    public var active: Bool
    public var directions: [String]
    public var activeDirection: String?

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(active: Bool, directions: [String], activeDirection: String?) {
        self.active = active
        self.directions = directions
        self.activeDirection = activeDirection
    }
}

extension LaneInfo: Equatable, Hashable {
    public static func == (lhs: LaneInfo, rhs: LaneInfo) -> Bool {
        if lhs.active != rhs.active {
            return false
        }
        if lhs.directions != rhs.directions {
            return false
        }
        if lhs.activeDirection != rhs.activeDirection {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(active)
        hasher.combine(directions)
        hasher.combine(activeDirection)
    }
}

public struct FfiConverterTypeLaneInfo: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> LaneInfo {
        try LaneInfo(
            active: FfiConverterBool.read(from: &buf),
            directions: FfiConverterSequenceString.read(from: &buf),
            activeDirection: FfiConverterOptionString.read(from: &buf)
        )
    }

    public static func write(_ value: LaneInfo, into buf: inout [UInt8]) {
        FfiConverterBool.write(value.active, into: &buf)
        FfiConverterSequenceString.write(value.directions, into: &buf)
        FfiConverterOptionString.write(value.activeDirection, into: &buf)
    }
}

public func FfiConverterTypeLaneInfo_lift(_ buf: RustBuffer) throws -> LaneInfo {
    try FfiConverterTypeLaneInfo.lift(buf)
}

public func FfiConverterTypeLaneInfo_lower(_ value: LaneInfo) -> RustBuffer {
    FfiConverterTypeLaneInfo.lower(value)
}

/**
 * The current state of the simulation.
 */
public struct LocationSimulationState {
    public var currentLocation: UserLocation
    public var remainingLocations: [GeographicCoordinate]

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(currentLocation: UserLocation, remainingLocations: [GeographicCoordinate]) {
        self.currentLocation = currentLocation
        self.remainingLocations = remainingLocations
    }
}

extension LocationSimulationState: Equatable, Hashable {
    public static func == (lhs: LocationSimulationState, rhs: LocationSimulationState) -> Bool {
        if lhs.currentLocation != rhs.currentLocation {
            return false
        }
        if lhs.remainingLocations != rhs.remainingLocations {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(currentLocation)
        hasher.combine(remainingLocations)
    }
}

public struct FfiConverterTypeLocationSimulationState: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> LocationSimulationState {
        try LocationSimulationState(
            currentLocation: FfiConverterTypeUserLocation.read(from: &buf),
            remainingLocations: FfiConverterSequenceTypeGeographicCoordinate.read(from: &buf)
        )
    }

    public static func write(_ value: LocationSimulationState, into buf: inout [UInt8]) {
        FfiConverterTypeUserLocation.write(value.currentLocation, into: &buf)
        FfiConverterSequenceTypeGeographicCoordinate.write(value.remainingLocations, into: &buf)
    }
}

public func FfiConverterTypeLocationSimulationState_lift(_ buf: RustBuffer) throws -> LocationSimulationState {
    try FfiConverterTypeLocationSimulationState.lift(buf)
}

public func FfiConverterTypeLocationSimulationState_lower(_ value: LocationSimulationState) -> RustBuffer {
    FfiConverterTypeLocationSimulationState.lower(value)
}

public struct NavigationControllerConfig {
    /**
     * Configures when navigation advances to the next step in the route.
     */
    public var stepAdvance: StepAdvanceMode
    /**
     * Configures when the user is deemed to be off course.
     *
     * NOTE: This is distinct from the action that is taken.
     * It is only the determination that the user has deviated from the expected route.
     */
    public var routeDeviationTracking: RouteDeviationTracking
    /**
     * Configures how the heading component of the snapped location is reported in [`TripState`].
     */
    public var snappedLocationCourseFiltering: CourseFiltering

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * Configures when navigation advances to the next step in the route.
         */ stepAdvance: StepAdvanceMode,
        /**
            * Configures when the user is deemed to be off course.
            *
            * NOTE: This is distinct from the action that is taken.
            * It is only the determination that the user has deviated from the expected route.
            */ routeDeviationTracking: RouteDeviationTracking,
        /**
            * Configures how the heading component of the snapped location is reported in [`TripState`].
            */ snappedLocationCourseFiltering: CourseFiltering
    ) {
        self.stepAdvance = stepAdvance
        self.routeDeviationTracking = routeDeviationTracking
        self.snappedLocationCourseFiltering = snappedLocationCourseFiltering
    }
}

public struct FfiConverterTypeNavigationControllerConfig: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> NavigationControllerConfig {
        try NavigationControllerConfig(
            stepAdvance: FfiConverterTypeStepAdvanceMode.read(from: &buf),
            routeDeviationTracking: FfiConverterTypeRouteDeviationTracking.read(from: &buf),
            snappedLocationCourseFiltering: FfiConverterTypeCourseFiltering.read(from: &buf)
        )
    }

    public static func write(_ value: NavigationControllerConfig, into buf: inout [UInt8]) {
        FfiConverterTypeStepAdvanceMode.write(value.stepAdvance, into: &buf)
        FfiConverterTypeRouteDeviationTracking.write(value.routeDeviationTracking, into: &buf)
        FfiConverterTypeCourseFiltering.write(value.snappedLocationCourseFiltering, into: &buf)
    }
}

public func FfiConverterTypeNavigationControllerConfig_lift(_ buf: RustBuffer) throws -> NavigationControllerConfig {
    try FfiConverterTypeNavigationControllerConfig.lift(buf)
}

public func FfiConverterTypeNavigationControllerConfig_lower(_ value: NavigationControllerConfig) -> RustBuffer {
    FfiConverterTypeNavigationControllerConfig.lower(value)
}

/**
 * Information describing the series of steps needed to travel between two or more points.
 *
 * NOTE: This type is unstable and is still under active development and should be
 * considered unstable.
 */
public struct Route {
    public var geometry: [GeographicCoordinate]
    public var bbox: BoundingBox
    /**
     * The total route distance, in meters.
     */
    public var distance: Double
    /**
     * The ordered list of waypoints to visit, including the starting point.
     * Note that this is distinct from the *geometry* which includes all points visited.
     * A waypoint represents a start/end point for a route leg.
     */
    public var waypoints: [Waypoint]
    public var steps: [RouteStep]

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(geometry: [GeographicCoordinate], bbox: BoundingBox,
                /**
                    * The total route distance, in meters.
                    */ distance: Double,
                /**
                    * The ordered list of waypoints to visit, including the starting point.
                    * Note that this is distinct from the *geometry* which includes all points visited.
                    * A waypoint represents a start/end point for a route leg.
                    */ waypoints: [Waypoint], steps: [RouteStep])
    {
        self.geometry = geometry
        self.bbox = bbox
        self.distance = distance
        self.waypoints = waypoints
        self.steps = steps
    }
}

extension Route: Equatable, Hashable {
    public static func == (lhs: Route, rhs: Route) -> Bool {
        if lhs.geometry != rhs.geometry {
            return false
        }
        if lhs.bbox != rhs.bbox {
            return false
        }
        if lhs.distance != rhs.distance {
            return false
        }
        if lhs.waypoints != rhs.waypoints {
            return false
        }
        if lhs.steps != rhs.steps {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(geometry)
        hasher.combine(bbox)
        hasher.combine(distance)
        hasher.combine(waypoints)
        hasher.combine(steps)
    }
}

public struct FfiConverterTypeRoute: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Route {
        try Route(
            geometry: FfiConverterSequenceTypeGeographicCoordinate.read(from: &buf),
            bbox: FfiConverterTypeBoundingBox.read(from: &buf),
            distance: FfiConverterDouble.read(from: &buf),
            waypoints: FfiConverterSequenceTypeWaypoint.read(from: &buf),
            steps: FfiConverterSequenceTypeRouteStep.read(from: &buf)
        )
    }

    public static func write(_ value: Route, into buf: inout [UInt8]) {
        FfiConverterSequenceTypeGeographicCoordinate.write(value.geometry, into: &buf)
        FfiConverterTypeBoundingBox.write(value.bbox, into: &buf)
        FfiConverterDouble.write(value.distance, into: &buf)
        FfiConverterSequenceTypeWaypoint.write(value.waypoints, into: &buf)
        FfiConverterSequenceTypeRouteStep.write(value.steps, into: &buf)
    }
}

public func FfiConverterTypeRoute_lift(_ buf: RustBuffer) throws -> Route {
    try FfiConverterTypeRoute.lift(buf)
}

public func FfiConverterTypeRoute_lower(_ value: Route) -> RustBuffer {
    FfiConverterTypeRoute.lower(value)
}

/**
 * A maneuver (such as a turn or merge) followed by travel of a certain distance until reaching
 * the next step.
 */
public struct RouteStep {
    /**
     * The full route geometry for this step.
     */
    public var geometry: [GeographicCoordinate]
    /**
     * The distance, in meters, to travel along the route after the maneuver to reach the next step.
     */
    public var distance: Double
    /**
     * The estimated duration, in seconds, that it will take to complete this step.
     */
    public var duration: Double
    /**
     * The name of the road being traveled on (useful for certain UI styles).
     */
    public var roadName: String?
    /**
     * A description of the maneuver (ex: "Turn wright onto main street").
     *
     * Note for UI implementers: the context this appears in (or doesn't)
     * depends somewhat on your use case and routing engine.
     * For example, this field is useful as a written instruction in Valhalla.
     */
    public var instruction: String
    /**
     * A list of instructions for visual display (usually as banners) at specific points along the step.
     */
    public var visualInstructions: [VisualInstruction]
    /**
     * A list of prompts to announce (via speech synthesis) at specific points along the step.
     */
    public var spokenInstructions: [SpokenInstruction]
    /**
     * A list of json encoded strings representing annotations between each coordinate along the step.
     */
    public var annotations: [String]?

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The full route geometry for this step.
         */ geometry: [GeographicCoordinate],
        /**
            * The distance, in meters, to travel along the route after the maneuver to reach the next step.
            */ distance: Double,
        /**
            * The estimated duration, in seconds, that it will take to complete this step.
            */ duration: Double,
        /**
            * The name of the road being traveled on (useful for certain UI styles).
            */ roadName: String?,
        /**
            * A description of the maneuver (ex: "Turn wright onto main street").
            *
            * Note for UI implementers: the context this appears in (or doesn't)
            * depends somewhat on your use case and routing engine.
            * For example, this field is useful as a written instruction in Valhalla.
            */ instruction: String,
        /**
            * A list of instructions for visual display (usually as banners) at specific points along the step.
            */ visualInstructions: [VisualInstruction],
        /**
            * A list of prompts to announce (via speech synthesis) at specific points along the step.
            */ spokenInstructions: [SpokenInstruction],
        /**
            * A list of json encoded strings representing annotations between each coordinate along the step.
            */ annotations: [String]?
    ) {
        self.geometry = geometry
        self.distance = distance
        self.duration = duration
        self.roadName = roadName
        self.instruction = instruction
        self.visualInstructions = visualInstructions
        self.spokenInstructions = spokenInstructions
        self.annotations = annotations
    }
}

extension RouteStep: Equatable, Hashable {
    public static func == (lhs: RouteStep, rhs: RouteStep) -> Bool {
        if lhs.geometry != rhs.geometry {
            return false
        }
        if lhs.distance != rhs.distance {
            return false
        }
        if lhs.duration != rhs.duration {
            return false
        }
        if lhs.roadName != rhs.roadName {
            return false
        }
        if lhs.instruction != rhs.instruction {
            return false
        }
        if lhs.visualInstructions != rhs.visualInstructions {
            return false
        }
        if lhs.spokenInstructions != rhs.spokenInstructions {
            return false
        }
        if lhs.annotations != rhs.annotations {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(geometry)
        hasher.combine(distance)
        hasher.combine(duration)
        hasher.combine(roadName)
        hasher.combine(instruction)
        hasher.combine(visualInstructions)
        hasher.combine(spokenInstructions)
        hasher.combine(annotations)
    }
}

public struct FfiConverterTypeRouteStep: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RouteStep {
        try RouteStep(
            geometry: FfiConverterSequenceTypeGeographicCoordinate.read(from: &buf),
            distance: FfiConverterDouble.read(from: &buf),
            duration: FfiConverterDouble.read(from: &buf),
            roadName: FfiConverterOptionString.read(from: &buf),
            instruction: FfiConverterString.read(from: &buf),
            visualInstructions: FfiConverterSequenceTypeVisualInstruction.read(from: &buf),
            spokenInstructions: FfiConverterSequenceTypeSpokenInstruction.read(from: &buf),
            annotations: FfiConverterOptionSequenceString.read(from: &buf)
        )
    }

    public static func write(_ value: RouteStep, into buf: inout [UInt8]) {
        FfiConverterSequenceTypeGeographicCoordinate.write(value.geometry, into: &buf)
        FfiConverterDouble.write(value.distance, into: &buf)
        FfiConverterDouble.write(value.duration, into: &buf)
        FfiConverterOptionString.write(value.roadName, into: &buf)
        FfiConverterString.write(value.instruction, into: &buf)
        FfiConverterSequenceTypeVisualInstruction.write(value.visualInstructions, into: &buf)
        FfiConverterSequenceTypeSpokenInstruction.write(value.spokenInstructions, into: &buf)
        FfiConverterOptionSequenceString.write(value.annotations, into: &buf)
    }
}

public func FfiConverterTypeRouteStep_lift(_ buf: RustBuffer) throws -> RouteStep {
    try FfiConverterTypeRouteStep.lift(buf)
}

public func FfiConverterTypeRouteStep_lower(_ value: RouteStep) -> RustBuffer {
    FfiConverterTypeRouteStep.lower(value)
}

/**
 * The speed of the user from the location provider.
 */
public struct Speed {
    /**
     * The user's speed in meters per second.
     */
    public var value: Double
    /**
     * The accuracy of the speed value, measured in meters per second.
     */
    public var accuracy: Double?

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The user's speed in meters per second.
         */ value: Double,
        /**
            * The accuracy of the speed value, measured in meters per second.
            */ accuracy: Double?
    ) {
        self.value = value
        self.accuracy = accuracy
    }
}

extension Speed: Equatable, Hashable {
    public static func == (lhs: Speed, rhs: Speed) -> Bool {
        if lhs.value != rhs.value {
            return false
        }
        if lhs.accuracy != rhs.accuracy {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(value)
        hasher.combine(accuracy)
    }
}

public struct FfiConverterTypeSpeed: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Speed {
        try Speed(
            value: FfiConverterDouble.read(from: &buf),
            accuracy: FfiConverterOptionDouble.read(from: &buf)
        )
    }

    public static func write(_ value: Speed, into buf: inout [UInt8]) {
        FfiConverterDouble.write(value.value, into: &buf)
        FfiConverterOptionDouble.write(value.accuracy, into: &buf)
    }
}

public func FfiConverterTypeSpeed_lift(_ buf: RustBuffer) throws -> Speed {
    try FfiConverterTypeSpeed.lift(buf)
}

public func FfiConverterTypeSpeed_lower(_ value: Speed) -> RustBuffer {
    FfiConverterTypeSpeed.lower(value)
}

/**
 * An instruction that can be synthesized using a TTS engine to announce an upcoming maneuver.
 *
 * Note that these do not have any locale information attached.
 */
public struct SpokenInstruction {
    /**
     * Plain-text instruction which can be synthesized with a TTS engine.
     */
    public var text: String
    /**
     * Speech Synthesis Markup Language, which should be preferred by clients capable of understanding it.
     */
    public var ssml: String?
    /**
     * How far (in meters) from the upcoming maneuver the instruction should start being displayed
     */
    public var triggerDistanceBeforeManeuver: Double
    /**
     * A unique identifier for this instruction.
     *
     * This is provided so that platform-layer integrations can easily disambiguate between distinct utterances,
     * which may have the same textual content.
     * UUIDs conveniently fill this purpose.
     *
     * NOTE: While it is possible to deterministically create UUIDs, we do not do so at this time.
     * This should be theoretically possible though if someone cares to write up a proposal and a PR.
     */
    public var utteranceId: Uuid

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * Plain-text instruction which can be synthesized with a TTS engine.
         */ text: String,
        /**
            * Speech Synthesis Markup Language, which should be preferred by clients capable of understanding it.
            */ ssml: String?,
        /**
            * How far (in meters) from the upcoming maneuver the instruction should start being displayed
            */ triggerDistanceBeforeManeuver: Double,
        /**
            * A unique identifier for this instruction.
            *
            * This is provided so that platform-layer integrations can easily disambiguate between distinct utterances,
            * which may have the same textual content.
            * UUIDs conveniently fill this purpose.
            *
            * NOTE: While it is possible to deterministically create UUIDs, we do not do so at this time.
            * This should be theoretically possible though if someone cares to write up a proposal and a PR.
            */ utteranceId: Uuid
    ) {
        self.text = text
        self.ssml = ssml
        self.triggerDistanceBeforeManeuver = triggerDistanceBeforeManeuver
        self.utteranceId = utteranceId
    }
}

extension SpokenInstruction: Equatable, Hashable {
    public static func == (lhs: SpokenInstruction, rhs: SpokenInstruction) -> Bool {
        if lhs.text != rhs.text {
            return false
        }
        if lhs.ssml != rhs.ssml {
            return false
        }
        if lhs.triggerDistanceBeforeManeuver != rhs.triggerDistanceBeforeManeuver {
            return false
        }
        if lhs.utteranceId != rhs.utteranceId {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(text)
        hasher.combine(ssml)
        hasher.combine(triggerDistanceBeforeManeuver)
        hasher.combine(utteranceId)
    }
}

public struct FfiConverterTypeSpokenInstruction: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SpokenInstruction {
        try SpokenInstruction(
            text: FfiConverterString.read(from: &buf),
            ssml: FfiConverterOptionString.read(from: &buf),
            triggerDistanceBeforeManeuver: FfiConverterDouble.read(from: &buf),
            utteranceId: FfiConverterTypeUuid.read(from: &buf)
        )
    }

    public static func write(_ value: SpokenInstruction, into buf: inout [UInt8]) {
        FfiConverterString.write(value.text, into: &buf)
        FfiConverterOptionString.write(value.ssml, into: &buf)
        FfiConverterDouble.write(value.triggerDistanceBeforeManeuver, into: &buf)
        FfiConverterTypeUuid.write(value.utteranceId, into: &buf)
    }
}

public func FfiConverterTypeSpokenInstruction_lift(_ buf: RustBuffer) throws -> SpokenInstruction {
    try FfiConverterTypeSpokenInstruction.lift(buf)
}

public func FfiConverterTypeSpokenInstruction_lower(_ value: SpokenInstruction) -> RustBuffer {
    FfiConverterTypeSpokenInstruction.lower(value)
}

/**
 * High-level state describing progress through a route.
 */
public struct TripProgress {
    /**
     * The distance to the next maneuver, in meters.
     */
    public var distanceToNextManeuver: Double
    /**
     * The total distance remaining in the trip, in meters.
     *
     * This is the sum of the distance remaining in the current step and the distance remaining in all subsequent steps.
     */
    public var distanceRemaining: Double
    /**
     * The total duration remaining in the trip, in seconds.
     */
    public var durationRemaining: Double

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The distance to the next maneuver, in meters.
         */ distanceToNextManeuver: Double,
        /**
            * The total distance remaining in the trip, in meters.
            *
            * This is the sum of the distance remaining in the current step and the distance remaining in all subsequent steps.
            */ distanceRemaining: Double,
        /**
            * The total duration remaining in the trip, in seconds.
            */ durationRemaining: Double
    ) {
        self.distanceToNextManeuver = distanceToNextManeuver
        self.distanceRemaining = distanceRemaining
        self.durationRemaining = durationRemaining
    }
}

extension TripProgress: Equatable, Hashable {
    public static func == (lhs: TripProgress, rhs: TripProgress) -> Bool {
        if lhs.distanceToNextManeuver != rhs.distanceToNextManeuver {
            return false
        }
        if lhs.distanceRemaining != rhs.distanceRemaining {
            return false
        }
        if lhs.durationRemaining != rhs.durationRemaining {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(distanceToNextManeuver)
        hasher.combine(distanceRemaining)
        hasher.combine(durationRemaining)
    }
}

public struct FfiConverterTypeTripProgress: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> TripProgress {
        try TripProgress(
            distanceToNextManeuver: FfiConverterDouble.read(from: &buf),
            distanceRemaining: FfiConverterDouble.read(from: &buf),
            durationRemaining: FfiConverterDouble.read(from: &buf)
        )
    }

    public static func write(_ value: TripProgress, into buf: inout [UInt8]) {
        FfiConverterDouble.write(value.distanceToNextManeuver, into: &buf)
        FfiConverterDouble.write(value.distanceRemaining, into: &buf)
        FfiConverterDouble.write(value.durationRemaining, into: &buf)
    }
}

public func FfiConverterTypeTripProgress_lift(_ buf: RustBuffer) throws -> TripProgress {
    try FfiConverterTypeTripProgress.lift(buf)
}

public func FfiConverterTypeTripProgress_lower(_ value: TripProgress) -> RustBuffer {
    FfiConverterTypeTripProgress.lower(value)
}

/**
 * The location of the user that is navigating.
 *
 * In addition to coordinates, this includes estimated accuracy and course information,
 * which can influence navigation logic and UI.
 *
 * NOTE: Heading is absent on purpose.
 * Heading updates are not related to a change in the user's location.
 */
public struct UserLocation {
    public var coordinates: GeographicCoordinate
    /**
     * The estimated accuracy of the coordinate (in meters)
     */
    public var horizontalAccuracy: Double
    public var courseOverGround: CourseOverGround?
    public var timestamp: Date
    public var speed: Speed?

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(coordinates: GeographicCoordinate,
                /**
                    * The estimated accuracy of the coordinate (in meters)
                    */ horizontalAccuracy: Double, courseOverGround: CourseOverGround?, timestamp: Date, speed: Speed?)
    {
        self.coordinates = coordinates
        self.horizontalAccuracy = horizontalAccuracy
        self.courseOverGround = courseOverGround
        self.timestamp = timestamp
        self.speed = speed
    }
}

extension UserLocation: Equatable, Hashable {
    public static func == (lhs: UserLocation, rhs: UserLocation) -> Bool {
        if lhs.coordinates != rhs.coordinates {
            return false
        }
        if lhs.horizontalAccuracy != rhs.horizontalAccuracy {
            return false
        }
        if lhs.courseOverGround != rhs.courseOverGround {
            return false
        }
        if lhs.timestamp != rhs.timestamp {
            return false
        }
        if lhs.speed != rhs.speed {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(coordinates)
        hasher.combine(horizontalAccuracy)
        hasher.combine(courseOverGround)
        hasher.combine(timestamp)
        hasher.combine(speed)
    }
}

public struct FfiConverterTypeUserLocation: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> UserLocation {
        try UserLocation(
            coordinates: FfiConverterTypeGeographicCoordinate.read(from: &buf),
            horizontalAccuracy: FfiConverterDouble.read(from: &buf),
            courseOverGround: FfiConverterOptionTypeCourseOverGround.read(from: &buf),
            timestamp: FfiConverterTimestamp.read(from: &buf),
            speed: FfiConverterOptionTypeSpeed.read(from: &buf)
        )
    }

    public static func write(_ value: UserLocation, into buf: inout [UInt8]) {
        FfiConverterTypeGeographicCoordinate.write(value.coordinates, into: &buf)
        FfiConverterDouble.write(value.horizontalAccuracy, into: &buf)
        FfiConverterOptionTypeCourseOverGround.write(value.courseOverGround, into: &buf)
        FfiConverterTimestamp.write(value.timestamp, into: &buf)
        FfiConverterOptionTypeSpeed.write(value.speed, into: &buf)
    }
}

public func FfiConverterTypeUserLocation_lift(_ buf: RustBuffer) throws -> UserLocation {
    try FfiConverterTypeUserLocation.lift(buf)
}

public func FfiConverterTypeUserLocation_lower(_ value: UserLocation) -> RustBuffer {
    FfiConverterTypeUserLocation.lower(value)
}

/**
 * An instruction for visual display (usually as banners) at a specific point along a [`RouteStep`].
 */
public struct VisualInstruction {
    /**
     * The primary instruction content.
     *
     * This is usually given more visual weight.
     */
    public var primaryContent: VisualInstructionContent
    /**
     * Optional secondary instruction content.
     */
    public var secondaryContent: VisualInstructionContent?
    /**
     * Optional sub-maneuver instruction content.
     */
    public var subContent: VisualInstructionContent?
    /**
     * How far (in meters) from the upcoming maneuver the instruction should start being displayed
     */
    public var triggerDistanceBeforeManeuver: Double

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The primary instruction content.
         *
         * This is usually given more visual weight.
         */ primaryContent: VisualInstructionContent,
        /**
            * Optional secondary instruction content.
            */ secondaryContent: VisualInstructionContent?,
        /**
            * Optional sub-maneuver instruction content.
            */ subContent: VisualInstructionContent?,
        /**
            * How far (in meters) from the upcoming maneuver the instruction should start being displayed
            */ triggerDistanceBeforeManeuver: Double
    ) {
        self.primaryContent = primaryContent
        self.secondaryContent = secondaryContent
        self.subContent = subContent
        self.triggerDistanceBeforeManeuver = triggerDistanceBeforeManeuver
    }
}

extension VisualInstruction: Equatable, Hashable {
    public static func == (lhs: VisualInstruction, rhs: VisualInstruction) -> Bool {
        if lhs.primaryContent != rhs.primaryContent {
            return false
        }
        if lhs.secondaryContent != rhs.secondaryContent {
            return false
        }
        if lhs.subContent != rhs.subContent {
            return false
        }
        if lhs.triggerDistanceBeforeManeuver != rhs.triggerDistanceBeforeManeuver {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(primaryContent)
        hasher.combine(secondaryContent)
        hasher.combine(subContent)
        hasher.combine(triggerDistanceBeforeManeuver)
    }
}

public struct FfiConverterTypeVisualInstruction: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> VisualInstruction {
        try VisualInstruction(
            primaryContent: FfiConverterTypeVisualInstructionContent.read(from: &buf),
            secondaryContent: FfiConverterOptionTypeVisualInstructionContent.read(from: &buf),
            subContent: FfiConverterOptionTypeVisualInstructionContent.read(from: &buf),
            triggerDistanceBeforeManeuver: FfiConverterDouble.read(from: &buf)
        )
    }

    public static func write(_ value: VisualInstruction, into buf: inout [UInt8]) {
        FfiConverterTypeVisualInstructionContent.write(value.primaryContent, into: &buf)
        FfiConverterOptionTypeVisualInstructionContent.write(value.secondaryContent, into: &buf)
        FfiConverterOptionTypeVisualInstructionContent.write(value.subContent, into: &buf)
        FfiConverterDouble.write(value.triggerDistanceBeforeManeuver, into: &buf)
    }
}

public func FfiConverterTypeVisualInstruction_lift(_ buf: RustBuffer) throws -> VisualInstruction {
    try FfiConverterTypeVisualInstruction.lift(buf)
}

public func FfiConverterTypeVisualInstruction_lower(_ value: VisualInstruction) -> RustBuffer {
    FfiConverterTypeVisualInstruction.lower(value)
}

/**
 * The content of a visual instruction.
 */
public struct VisualInstructionContent {
    /**
     * The text to display.
     */
    public var text: String
    /**
     * A standardized maneuver type (if any).
     */
    public var maneuverType: ManeuverType?
    /**
     * A standardized maneuver modifier (if any).
     */
    public var maneuverModifier: ManeuverModifier?
    /**
     * If applicable, the number of degrees you need to go around the roundabout before exiting.
     *
     * For example, entering and exiting the roundabout in the same direction of travel
     * (as if you had gone straight, apart from the detour)
     * would be an exit angle of 180 degrees.
     */
    public var roundaboutExitDegrees: UInt16?
    /**
     * Detailed information about the lanes. This is typically only present in sub-maneuver instructions.
     */
    public var laneInfo: [LaneInfo]?

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        /**
         * The text to display.
         */ text: String,
        /**
            * A standardized maneuver type (if any).
            */ maneuverType: ManeuverType?,
        /**
            * A standardized maneuver modifier (if any).
            */ maneuverModifier: ManeuverModifier?,
        /**
            * If applicable, the number of degrees you need to go around the roundabout before exiting.
            *
            * For example, entering and exiting the roundabout in the same direction of travel
            * (as if you had gone straight, apart from the detour)
            * would be an exit angle of 180 degrees.
            */ roundaboutExitDegrees: UInt16?,
        /**
            * Detailed information about the lanes. This is typically only present in sub-maneuver instructions.
            */ laneInfo: [LaneInfo]?
    ) {
        self.text = text
        self.maneuverType = maneuverType
        self.maneuverModifier = maneuverModifier
        self.roundaboutExitDegrees = roundaboutExitDegrees
        self.laneInfo = laneInfo
    }
}

extension VisualInstructionContent: Equatable, Hashable {
    public static func == (lhs: VisualInstructionContent, rhs: VisualInstructionContent) -> Bool {
        if lhs.text != rhs.text {
            return false
        }
        if lhs.maneuverType != rhs.maneuverType {
            return false
        }
        if lhs.maneuverModifier != rhs.maneuverModifier {
            return false
        }
        if lhs.roundaboutExitDegrees != rhs.roundaboutExitDegrees {
            return false
        }
        if lhs.laneInfo != rhs.laneInfo {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(text)
        hasher.combine(maneuverType)
        hasher.combine(maneuverModifier)
        hasher.combine(roundaboutExitDegrees)
        hasher.combine(laneInfo)
    }
}

public struct FfiConverterTypeVisualInstructionContent: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> VisualInstructionContent {
        try VisualInstructionContent(
            text: FfiConverterString.read(from: &buf),
            maneuverType: FfiConverterOptionTypeManeuverType.read(from: &buf),
            maneuverModifier: FfiConverterOptionTypeManeuverModifier.read(from: &buf),
            roundaboutExitDegrees: FfiConverterOptionUInt16.read(from: &buf),
            laneInfo: FfiConverterOptionSequenceTypeLaneInfo.read(from: &buf)
        )
    }

    public static func write(_ value: VisualInstructionContent, into buf: inout [UInt8]) {
        FfiConverterString.write(value.text, into: &buf)
        FfiConverterOptionTypeManeuverType.write(value.maneuverType, into: &buf)
        FfiConverterOptionTypeManeuverModifier.write(value.maneuverModifier, into: &buf)
        FfiConverterOptionUInt16.write(value.roundaboutExitDegrees, into: &buf)
        FfiConverterOptionSequenceTypeLaneInfo.write(value.laneInfo, into: &buf)
    }
}

public func FfiConverterTypeVisualInstructionContent_lift(_ buf: RustBuffer) throws -> VisualInstructionContent {
    try FfiConverterTypeVisualInstructionContent.lift(buf)
}

public func FfiConverterTypeVisualInstructionContent_lower(_ value: VisualInstructionContent) -> RustBuffer {
    FfiConverterTypeVisualInstructionContent.lower(value)
}

/**
 * A waypoint along a route.
 *
 * Within the context of Ferrostar, a route request consists of exactly one [`UserLocation`]
 * and at least one [`Waypoint`]. The route starts from the user's location (which may
 * contain other useful information like their current course for the [`crate::routing_adapters::RouteRequestGenerator`]
 * to use) and proceeds through one or more waypoints.
 *
 * Waypoints are used during route calculation, are tracked throughout the lifecycle of a trip,
 * and are used for recalculating when the user deviates from the expected route.
 *
 * Note that support for properties beyond basic geographic coordinates varies by routing engine.
 */
public struct Waypoint {
    public var coordinate: GeographicCoordinate
    public var kind: WaypointKind

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(coordinate: GeographicCoordinate, kind: WaypointKind) {
        self.coordinate = coordinate
        self.kind = kind
    }
}

extension Waypoint: Equatable, Hashable {
    public static func == (lhs: Waypoint, rhs: Waypoint) -> Bool {
        if lhs.coordinate != rhs.coordinate {
            return false
        }
        if lhs.kind != rhs.kind {
            return false
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(coordinate)
        hasher.combine(kind)
    }
}

public struct FfiConverterTypeWaypoint: FfiConverterRustBuffer {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Waypoint {
        try Waypoint(
            coordinate: FfiConverterTypeGeographicCoordinate.read(from: &buf),
            kind: FfiConverterTypeWaypointKind.read(from: &buf)
        )
    }

    public static func write(_ value: Waypoint, into buf: inout [UInt8]) {
        FfiConverterTypeGeographicCoordinate.write(value.coordinate, into: &buf)
        FfiConverterTypeWaypointKind.write(value.kind, into: &buf)
    }
}

public func FfiConverterTypeWaypoint_lift(_ buf: RustBuffer) throws -> Waypoint {
    try FfiConverterTypeWaypoint.lift(buf)
}

public func FfiConverterTypeWaypoint_lower(_ value: Waypoint) -> RustBuffer {
    FfiConverterTypeWaypoint.lower(value)
}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * Controls filtering/post-processing of user course by the [`NavigationController`].
 */

public enum CourseFiltering {
    /**
     * Snap the user's course to the current step's linestring using the next index in the step's geometry.

     */
    case snapToRoute
    /**
     * Use the raw course as reported by the location provider with no processing.
     */
    case raw
}

public struct FfiConverterTypeCourseFiltering: FfiConverterRustBuffer {
    typealias SwiftType = CourseFiltering

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> CourseFiltering {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .snapToRoute

        case 2: return .raw

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: CourseFiltering, into buf: inout [UInt8]) {
        switch value {
        case .snapToRoute:
            writeInt(&buf, Int32(1))

        case .raw:
            writeInt(&buf, Int32(2))
        }
    }
}

public func FfiConverterTypeCourseFiltering_lift(_ buf: RustBuffer) throws -> CourseFiltering {
    try FfiConverterTypeCourseFiltering.lift(buf)
}

public func FfiConverterTypeCourseFiltering_lower(_ value: CourseFiltering) -> RustBuffer {
    FfiConverterTypeCourseFiltering.lower(value)
}

extension CourseFiltering: Equatable, Hashable {}

public enum InstantiationError {
    case OptionsJsonParseError
}

public struct FfiConverterTypeInstantiationError: FfiConverterRustBuffer {
    typealias SwiftType = InstantiationError

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> InstantiationError {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .OptionsJsonParseError

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: InstantiationError, into buf: inout [UInt8]) {
        switch value {
        case .OptionsJsonParseError:
            writeInt(&buf, Int32(1))
        }
    }
}

extension InstantiationError: Equatable, Hashable {}

extension InstantiationError: Foundation.LocalizedError {
    public var errorDescription: String? {
        String(reflecting: self)
    }
}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * Additional information to further specify a [`ManeuverType`].
 */

public enum ManeuverModifier {
    case uTurn
    case sharpRight
    case right
    case slightRight
    case straight
    case slightLeft
    case left
    case sharpLeft
}

public struct FfiConverterTypeManeuverModifier: FfiConverterRustBuffer {
    typealias SwiftType = ManeuverModifier

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> ManeuverModifier {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .uTurn

        case 2: return .sharpRight

        case 3: return .right

        case 4: return .slightRight

        case 5: return .straight

        case 6: return .slightLeft

        case 7: return .left

        case 8: return .sharpLeft

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: ManeuverModifier, into buf: inout [UInt8]) {
        switch value {
        case .uTurn:
            writeInt(&buf, Int32(1))

        case .sharpRight:
            writeInt(&buf, Int32(2))

        case .right:
            writeInt(&buf, Int32(3))

        case .slightRight:
            writeInt(&buf, Int32(4))

        case .straight:
            writeInt(&buf, Int32(5))

        case .slightLeft:
            writeInt(&buf, Int32(6))

        case .left:
            writeInt(&buf, Int32(7))

        case .sharpLeft:
            writeInt(&buf, Int32(8))
        }
    }
}

public func FfiConverterTypeManeuverModifier_lift(_ buf: RustBuffer) throws -> ManeuverModifier {
    try FfiConverterTypeManeuverModifier.lift(buf)
}

public func FfiConverterTypeManeuverModifier_lower(_ value: ManeuverModifier) -> RustBuffer {
    FfiConverterTypeManeuverModifier.lower(value)
}

extension ManeuverModifier: Equatable, Hashable {}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * The broad class of maneuver to perform.
 *
 * This is usually combined with [`ManeuverModifier`] in [`VisualInstructionContent`].
 */

public enum ManeuverType {
    case turn
    case newName
    case depart
    case arrive
    case merge
    case onRamp
    case offRamp
    case fork
    case endOfRoad
    case `continue`
    case roundabout
    case rotary
    case roundaboutTurn
    case notification
    case exitRoundabout
    case exitRotary
}

public struct FfiConverterTypeManeuverType: FfiConverterRustBuffer {
    typealias SwiftType = ManeuverType

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> ManeuverType {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .turn

        case 2: return .newName

        case 3: return .depart

        case 4: return .arrive

        case 5: return .merge

        case 6: return .onRamp

        case 7: return .offRamp

        case 8: return .fork

        case 9: return .endOfRoad

        case 10: return .continue

        case 11: return .roundabout

        case 12: return .rotary

        case 13: return .roundaboutTurn

        case 14: return .notification

        case 15: return .exitRoundabout

        case 16: return .exitRotary

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: ManeuverType, into buf: inout [UInt8]) {
        switch value {
        case .turn:
            writeInt(&buf, Int32(1))

        case .newName:
            writeInt(&buf, Int32(2))

        case .depart:
            writeInt(&buf, Int32(3))

        case .arrive:
            writeInt(&buf, Int32(4))

        case .merge:
            writeInt(&buf, Int32(5))

        case .onRamp:
            writeInt(&buf, Int32(6))

        case .offRamp:
            writeInt(&buf, Int32(7))

        case .fork:
            writeInt(&buf, Int32(8))

        case .endOfRoad:
            writeInt(&buf, Int32(9))

        case .continue:
            writeInt(&buf, Int32(10))

        case .roundabout:
            writeInt(&buf, Int32(11))

        case .rotary:
            writeInt(&buf, Int32(12))

        case .roundaboutTurn:
            writeInt(&buf, Int32(13))

        case .notification:
            writeInt(&buf, Int32(14))

        case .exitRoundabout:
            writeInt(&buf, Int32(15))

        case .exitRotary:
            writeInt(&buf, Int32(16))
        }
    }
}

public func FfiConverterTypeManeuverType_lift(_ buf: RustBuffer) throws -> ManeuverType {
    try FfiConverterTypeManeuverType.lift(buf)
}

public func FfiConverterTypeManeuverType_lower(_ value: ManeuverType) -> RustBuffer {
    FfiConverterTypeManeuverType.lower(value)
}

extension ManeuverType: Equatable, Hashable {}

public enum ModelError {
    case PolylineGenerationError(error: String)
}

public struct FfiConverterTypeModelError: FfiConverterRustBuffer {
    typealias SwiftType = ModelError

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> ModelError {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return try .PolylineGenerationError(
                error: FfiConverterString.read(from: &buf)
            )

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: ModelError, into buf: inout [UInt8]) {
        switch value {
        case let .PolylineGenerationError(error):
            writeInt(&buf, Int32(1))
            FfiConverterString.write(error, into: &buf)
        }
    }
}

extension ModelError: Equatable, Hashable {}

extension ModelError: Foundation.LocalizedError {
    public var errorDescription: String? {
        String(reflecting: self)
    }
}

public enum ParsingError {
    case InvalidRouteObject(error: String)
    case InvalidGeometry(error: String)
    case MalformedAnnotations(error: String)
    case InvalidStatusCode(code: String)
    case UnknownParsingError
}

public struct FfiConverterTypeParsingError: FfiConverterRustBuffer {
    typealias SwiftType = ParsingError

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> ParsingError {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return try .InvalidRouteObject(
                error: FfiConverterString.read(from: &buf)
            )
        case 2: return try .InvalidGeometry(
                error: FfiConverterString.read(from: &buf)
            )
        case 3: return try .MalformedAnnotations(
                error: FfiConverterString.read(from: &buf)
            )
        case 4: return try .InvalidStatusCode(
                code: FfiConverterString.read(from: &buf)
            )
        case 5: return .UnknownParsingError
        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: ParsingError, into buf: inout [UInt8]) {
        switch value {
        case let .InvalidRouteObject(error):
            writeInt(&buf, Int32(1))
            FfiConverterString.write(error, into: &buf)

        case let .InvalidGeometry(error):
            writeInt(&buf, Int32(2))
            FfiConverterString.write(error, into: &buf)

        case let .MalformedAnnotations(error):
            writeInt(&buf, Int32(3))
            FfiConverterString.write(error, into: &buf)

        case let .InvalidStatusCode(code):
            writeInt(&buf, Int32(4))
            FfiConverterString.write(code, into: &buf)

        case .UnknownParsingError:
            writeInt(&buf, Int32(5))
        }
    }
}

extension ParsingError: Equatable, Hashable {}

extension ParsingError: Foundation.LocalizedError {
    public var errorDescription: String? {
        String(reflecting: self)
    }
}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * Status information that describes whether the user is proceeding according to the route or not.
 *
 * Note that the name is intentionally a bit generic to allow for expansion of other states.
 * For example, we could conceivably add a "wrong way" status in the future.
 */

public enum RouteDeviation {
    /**
     * The user is proceeding on course within the expected tolerances; everything is normal.
     */
    case noDeviation
    /**
     * The user is off the expected route.
     */
    case offRoute(
        /**
         * The deviation from the route line, in meters.
         */ deviationFromRouteLine: Double
    )
}

public struct FfiConverterTypeRouteDeviation: FfiConverterRustBuffer {
    typealias SwiftType = RouteDeviation

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RouteDeviation {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .noDeviation

        case 2: return try .offRoute(deviationFromRouteLine: FfiConverterDouble.read(from: &buf))

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: RouteDeviation, into buf: inout [UInt8]) {
        switch value {
        case .noDeviation:
            writeInt(&buf, Int32(1))

        case let .offRoute(deviationFromRouteLine):
            writeInt(&buf, Int32(2))
            FfiConverterDouble.write(deviationFromRouteLine, into: &buf)
        }
    }
}

public func FfiConverterTypeRouteDeviation_lift(_ buf: RustBuffer) throws -> RouteDeviation {
    try FfiConverterTypeRouteDeviation.lift(buf)
}

public func FfiConverterTypeRouteDeviation_lower(_ value: RouteDeviation) -> RustBuffer {
    FfiConverterTypeRouteDeviation.lower(value)
}

extension RouteDeviation: Equatable, Hashable {}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * Determines if the user has deviated from the expected route.
 */

public enum RouteDeviationTracking {
    /**
     * No checks will be done, and we assume the user is always following the route.
     */
    case none
    /**
     * Detects deviation from the route using a configurable static distance threshold from the route line.
     */
    case staticThreshold(
        /**
         * The minimum required horizontal accuracy of the user location, in meters.
         * Values larger than this will not trigger route deviation warnings.
         */ minimumHorizontalAccuracy: UInt16,
        /**
            * The maximum acceptable deviation from the route line, in meters.
            *
            * If the distance between the reported location and the expected route line
            * is greater than this threshold, it will be flagged as an off route condition.
            */ maxAcceptableDeviation: Double
    )
    /**
     * An arbitrary user-defined implementation.
     * You decide with your own [`RouteDeviationDetector`] implementation!
     */
    case custom(detector: RouteDeviationDetector)
}

public struct FfiConverterTypeRouteDeviationTracking: FfiConverterRustBuffer {
    typealias SwiftType = RouteDeviationTracking

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RouteDeviationTracking {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .none

        case 2: return try .staticThreshold(
                minimumHorizontalAccuracy: FfiConverterUInt16.read(from: &buf),
                maxAcceptableDeviation: FfiConverterDouble.read(from: &buf)
            )

        case 3: return try .custom(detector: FfiConverterTypeRouteDeviationDetector.read(from: &buf))

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: RouteDeviationTracking, into buf: inout [UInt8]) {
        switch value {
        case .none:
            writeInt(&buf, Int32(1))

        case let .staticThreshold(minimumHorizontalAccuracy, maxAcceptableDeviation):
            writeInt(&buf, Int32(2))
            FfiConverterUInt16.write(minimumHorizontalAccuracy, into: &buf)
            FfiConverterDouble.write(maxAcceptableDeviation, into: &buf)

        case let .custom(detector):
            writeInt(&buf, Int32(3))
            FfiConverterTypeRouteDeviationDetector.write(detector, into: &buf)
        }
    }
}

public func FfiConverterTypeRouteDeviationTracking_lift(_ buf: RustBuffer) throws -> RouteDeviationTracking {
    try FfiConverterTypeRouteDeviationTracking.lift(buf)
}

public func FfiConverterTypeRouteDeviationTracking_lower(_ value: RouteDeviationTracking) -> RustBuffer {
    FfiConverterTypeRouteDeviationTracking.lower(value)
}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * A route request generated by a [`RouteRequestGenerator`].
 */

public enum RouteRequest {
    case httpPost(url: String, headers: [String: String], body: Data)
    case httpGet(url: String, headers: [String: String])
}

public struct FfiConverterTypeRouteRequest: FfiConverterRustBuffer {
    typealias SwiftType = RouteRequest

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RouteRequest {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return try .httpPost(
                url: FfiConverterString.read(from: &buf),
                headers: FfiConverterDictionaryStringString.read(from: &buf),
                body: FfiConverterData.read(from: &buf)
            )

        case 2: return try .httpGet(
                url: FfiConverterString.read(from: &buf),
                headers: FfiConverterDictionaryStringString.read(from: &buf)
            )

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: RouteRequest, into buf: inout [UInt8]) {
        switch value {
        case let .httpPost(url, headers, body):
            writeInt(&buf, Int32(1))
            FfiConverterString.write(url, into: &buf)
            FfiConverterDictionaryStringString.write(headers, into: &buf)
            FfiConverterData.write(body, into: &buf)

        case let .httpGet(url, headers):
            writeInt(&buf, Int32(2))
            FfiConverterString.write(url, into: &buf)
            FfiConverterDictionaryStringString.write(headers, into: &buf)
        }
    }
}

public func FfiConverterTypeRouteRequest_lift(_ buf: RustBuffer) throws -> RouteRequest {
    try FfiConverterTypeRouteRequest.lift(buf)
}

public func FfiConverterTypeRouteRequest_lower(_ value: RouteRequest) -> RustBuffer {
    FfiConverterTypeRouteRequest.lower(value)
}

extension RouteRequest: Equatable, Hashable {}

public enum RoutingRequestGenerationError {
    case NotEnoughWaypoints
    case JsonError
    case UnknownRequestGenerationError
}

public struct FfiConverterTypeRoutingRequestGenerationError: FfiConverterRustBuffer {
    typealias SwiftType = RoutingRequestGenerationError

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> RoutingRequestGenerationError {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .NotEnoughWaypoints
        case 2: return .JsonError
        case 3: return .UnknownRequestGenerationError
        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: RoutingRequestGenerationError, into buf: inout [UInt8]) {
        switch value {
        case .NotEnoughWaypoints:
            writeInt(&buf, Int32(1))

        case .JsonError:
            writeInt(&buf, Int32(2))

        case .UnknownRequestGenerationError:
            writeInt(&buf, Int32(3))
        }
    }
}

extension RoutingRequestGenerationError: Equatable, Hashable {}

extension RoutingRequestGenerationError: Foundation.LocalizedError {
    public var errorDescription: String? {
        String(reflecting: self)
    }
}

public enum SimulationError {
    /**
     * Errors decoding the polyline string.
     */
    case PolylineError(error: String)
    /**
     * Not enough points in the input.
     */
    case NotEnoughPoints
}

public struct FfiConverterTypeSimulationError: FfiConverterRustBuffer {
    typealias SwiftType = SimulationError

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SimulationError {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return try .PolylineError(
                error: FfiConverterString.read(from: &buf)
            )

        case 2: return .NotEnoughPoints

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: SimulationError, into buf: inout [UInt8]) {
        switch value {
        case let .PolylineError(error):
            writeInt(&buf, Int32(1))
            FfiConverterString.write(error, into: &buf)

        case .NotEnoughPoints:
            writeInt(&buf, Int32(2))
        }
    }
}

extension SimulationError: Equatable, Hashable {}

extension SimulationError: Foundation.LocalizedError {
    public var errorDescription: String? {
        String(reflecting: self)
    }
}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * The step advance mode describes when the current maneuver has been successfully completed,
 * and we should advance to the next step.
 */

public enum StepAdvanceMode {
    /**
     * Never advances to the next step automatically;
     * requires calling [`NavigationController::advance_to_next_step`](super::NavigationController::advance_to_next_step).
     *
     * You can use this to implement custom behaviors in external code.
     */
    case manual
    /**
     * Automatically advances when the user's location is close enough to the end of the step
     */
    case distanceToEndOfStep(
        /**
         * Distance to the last waypoint in the step, measured in meters, at which to advance.
         */ distance: UInt16,
        /**
            * The minimum required horizontal accuracy of the user location, in meters.
            * Values larger than this cannot trigger a step advance.
            */ minimumHorizontalAccuracy: UInt16
    )
    /**
     * Automatically advances when the user's distance to the *next* step's linestring  is less
     * than the distance to the current step's linestring.
     */
    case relativeLineStringDistance(
        /**
         * The minimum required horizontal accuracy of the user location, in meters.
         * Values larger than this cannot trigger a step advance.
         */ minimumHorizontalAccuracy: UInt16,
        /**
            * At this (optional) distance, navigation should advance to the next step regardless
            * of which `LineString` appears closer.
            */ automaticAdvanceDistance: UInt16?
    )
}

public struct FfiConverterTypeStepAdvanceMode: FfiConverterRustBuffer {
    typealias SwiftType = StepAdvanceMode

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> StepAdvanceMode {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .manual

        case 2: return try .distanceToEndOfStep(
                distance: FfiConverterUInt16.read(from: &buf),
                minimumHorizontalAccuracy: FfiConverterUInt16.read(from: &buf)
            )

        case 3: return try .relativeLineStringDistance(
                minimumHorizontalAccuracy: FfiConverterUInt16.read(from: &buf),
                automaticAdvanceDistance: FfiConverterOptionUInt16.read(from: &buf)
            )

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: StepAdvanceMode, into buf: inout [UInt8]) {
        switch value {
        case .manual:
            writeInt(&buf, Int32(1))

        case let .distanceToEndOfStep(distance, minimumHorizontalAccuracy):
            writeInt(&buf, Int32(2))
            FfiConverterUInt16.write(distance, into: &buf)
            FfiConverterUInt16.write(minimumHorizontalAccuracy, into: &buf)

        case let .relativeLineStringDistance(minimumHorizontalAccuracy, automaticAdvanceDistance):
            writeInt(&buf, Int32(3))
            FfiConverterUInt16.write(minimumHorizontalAccuracy, into: &buf)
            FfiConverterOptionUInt16.write(automaticAdvanceDistance, into: &buf)
        }
    }
}

public func FfiConverterTypeStepAdvanceMode_lift(_ buf: RustBuffer) throws -> StepAdvanceMode {
    try FfiConverterTypeStepAdvanceMode.lift(buf)
}

public func FfiConverterTypeStepAdvanceMode_lower(_ value: StepAdvanceMode) -> RustBuffer {
    FfiConverterTypeStepAdvanceMode.lower(value)
}

extension StepAdvanceMode: Equatable, Hashable {}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * The state of a navigation session.
 *
 * This is produced by [`NavigationController`](super::NavigationController) methods
 * including [`get_initial_state`](super::NavigationController::get_initial_state)
 * and [`update_user_location`](super::NavigationController::update_user_location).
 */

public enum TripState {
    /**
     * The navigation controller is idle and there is no active trip.
     */
    case idle
    /**
     * The navigation controller is actively navigating a trip.
     */
    case navigating(
        /**
         * The index of the closest coordinate to the user's snapped location.
         *
         * This index is relative to the *current* [`RouteStep`]'s geometry.
         */ currentStepGeometryIndex: UInt64?,
        /**
            * A location on the line string that
            */ snappedUserLocation: UserLocation,
        /**
            * The ordered list of steps that remain in the trip.
            *
            * The step at the front of the list is always the current step.
            * We currently assume that you cannot move backward to a previous step.
            */ remainingSteps: [RouteStep],
        /**
            * Remaining waypoints to visit on the route.
            *
            * The waypoint at the front of the list is always the *next* waypoint "goal."
            * Unlike the current step, there is no value in tracking the "current" waypoint,
            * as the main use of waypoints is recalculation when the user deviates from the route.
            * (In most use cases, a route will have only two waypoints, but more complex use cases
            * may have multiple intervening points that are visited along the route.)
            * This list is updated as the user advances through the route.
            */ remainingWaypoints: [Waypoint],
        /**
            * The trip progress includes information that is useful for showing the
            * user's progress along the full navigation trip, the route and its components.
            */ progress: TripProgress,
        /**
            * The route deviation status: is the user following the route or not?
            */ deviation: RouteDeviation,
        /**
            * The visual instruction that should be displayed in the user interface.
            */ visualInstruction: VisualInstruction?,
        /**
            * The most recent spoken instruction that should be synthesized using TTS.
            *
            * Note it is the responsibility of the platform layer to ensure that utterances are not synthesized multiple times. This property simply reports the current spoken instruction.
            */ spokenInstruction: SpokenInstruction?,
        /**
            * Annotation data at the current location.
            * This is represented as a json formatted byte array to allow for flexible encoding of custom annotations.
            */ annotationJson: String?
    )
    /**
     * The navigation controller has reached the end of the trip.
     */
    case complete
}

public struct FfiConverterTypeTripState: FfiConverterRustBuffer {
    typealias SwiftType = TripState

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> TripState {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .idle

        case 2: return try .navigating(
                currentStepGeometryIndex: FfiConverterOptionUInt64.read(from: &buf),
                snappedUserLocation: FfiConverterTypeUserLocation.read(from: &buf),
                remainingSteps: FfiConverterSequenceTypeRouteStep.read(from: &buf),
                remainingWaypoints: FfiConverterSequenceTypeWaypoint.read(from: &buf),
                progress: FfiConverterTypeTripProgress.read(from: &buf),
                deviation: FfiConverterTypeRouteDeviation.read(from: &buf),
                visualInstruction: FfiConverterOptionTypeVisualInstruction.read(from: &buf),
                spokenInstruction: FfiConverterOptionTypeSpokenInstruction.read(from: &buf),
                annotationJson: FfiConverterOptionString.read(from: &buf)
            )

        case 3: return .complete

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: TripState, into buf: inout [UInt8]) {
        switch value {
        case .idle:
            writeInt(&buf, Int32(1))

        case let .navigating(
            currentStepGeometryIndex,
            snappedUserLocation,
            remainingSteps,
            remainingWaypoints,
            progress,
            deviation,
            visualInstruction,
            spokenInstruction,
            annotationJson
        ):
            writeInt(&buf, Int32(2))
            FfiConverterOptionUInt64.write(currentStepGeometryIndex, into: &buf)
            FfiConverterTypeUserLocation.write(snappedUserLocation, into: &buf)
            FfiConverterSequenceTypeRouteStep.write(remainingSteps, into: &buf)
            FfiConverterSequenceTypeWaypoint.write(remainingWaypoints, into: &buf)
            FfiConverterTypeTripProgress.write(progress, into: &buf)
            FfiConverterTypeRouteDeviation.write(deviation, into: &buf)
            FfiConverterOptionTypeVisualInstruction.write(visualInstruction, into: &buf)
            FfiConverterOptionTypeSpokenInstruction.write(spokenInstruction, into: &buf)
            FfiConverterOptionString.write(annotationJson, into: &buf)

        case .complete:
            writeInt(&buf, Int32(3))
        }
    }
}

public func FfiConverterTypeTripState_lift(_ buf: RustBuffer) throws -> TripState {
    try FfiConverterTypeTripState.lift(buf)
}

public func FfiConverterTypeTripState_lower(_ value: TripState) -> RustBuffer {
    FfiConverterTypeTripState.lower(value)
}

extension TripState: Equatable, Hashable {}

// Note that we don't yet support `indirect` for enums.
// See https://github.com/mozilla/uniffi-rs/issues/396 for further discussion.
/**
 * Describes characteristics of the waypoint for the routing backend.
 */

public enum WaypointKind {
    /**
     * Starts or ends a leg of the trip.
     *
     * Most routing engines will generate arrival and departure instructions.
     */
    case `break`
    /**
     * A waypoint that is simply passed through, but will not have any arrival or departure instructions.
     */
    case via
}

public struct FfiConverterTypeWaypointKind: FfiConverterRustBuffer {
    typealias SwiftType = WaypointKind

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> WaypointKind {
        let variant: Int32 = try readInt(&buf)
        switch variant {
        case 1: return .break

        case 2: return .via

        default: throw UniffiInternalError.unexpectedEnumCase
        }
    }

    public static func write(_ value: WaypointKind, into buf: inout [UInt8]) {
        switch value {
        case .break:
            writeInt(&buf, Int32(1))

        case .via:
            writeInt(&buf, Int32(2))
        }
    }
}

public func FfiConverterTypeWaypointKind_lift(_ buf: RustBuffer) throws -> WaypointKind {
    try FfiConverterTypeWaypointKind.lift(buf)
}

public func FfiConverterTypeWaypointKind_lower(_ value: WaypointKind) -> RustBuffer {
    FfiConverterTypeWaypointKind.lower(value)
}

extension WaypointKind: Equatable, Hashable {}

private struct FfiConverterOptionUInt16: FfiConverterRustBuffer {
    typealias SwiftType = UInt16?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterUInt16.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterUInt16.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionUInt64: FfiConverterRustBuffer {
    typealias SwiftType = UInt64?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterUInt64.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterUInt64.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionDouble: FfiConverterRustBuffer {
    typealias SwiftType = Double?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterDouble.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterDouble.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionString: FfiConverterRustBuffer {
    typealias SwiftType = String?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterString.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterString.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionTypeCourseOverGround: FfiConverterRustBuffer {
    typealias SwiftType = CourseOverGround?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterTypeCourseOverGround.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterTypeCourseOverGround.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionTypeSpeed: FfiConverterRustBuffer {
    typealias SwiftType = Speed?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterTypeSpeed.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterTypeSpeed.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionTypeSpokenInstruction: FfiConverterRustBuffer {
    typealias SwiftType = SpokenInstruction?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterTypeSpokenInstruction.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterTypeSpokenInstruction.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionTypeVisualInstruction: FfiConverterRustBuffer {
    typealias SwiftType = VisualInstruction?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterTypeVisualInstruction.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterTypeVisualInstruction.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionTypeVisualInstructionContent: FfiConverterRustBuffer {
    typealias SwiftType = VisualInstructionContent?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterTypeVisualInstructionContent.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterTypeVisualInstructionContent.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionTypeManeuverModifier: FfiConverterRustBuffer {
    typealias SwiftType = ManeuverModifier?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterTypeManeuverModifier.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterTypeManeuverModifier.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionTypeManeuverType: FfiConverterRustBuffer {
    typealias SwiftType = ManeuverType?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterTypeManeuverType.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterTypeManeuverType.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionSequenceString: FfiConverterRustBuffer {
    typealias SwiftType = [String]?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterSequenceString.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterSequenceString.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterOptionSequenceTypeLaneInfo: FfiConverterRustBuffer {
    typealias SwiftType = [LaneInfo]?

    public static func write(_ value: SwiftType, into buf: inout [UInt8]) {
        guard let value else {
            writeInt(&buf, Int8(0))
            return
        }
        writeInt(&buf, Int8(1))
        FfiConverterSequenceTypeLaneInfo.write(value, into: &buf)
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> SwiftType {
        switch try readInt(&buf) as Int8 {
        case 0: return nil
        case 1: return try FfiConverterSequenceTypeLaneInfo.read(from: &buf)
        default: throw UniffiInternalError.unexpectedOptionalTag
        }
    }
}

private struct FfiConverterSequenceString: FfiConverterRustBuffer {
    typealias SwiftType = [String]

    public static func write(_ value: [String], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for item in value {
            FfiConverterString.write(item, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [String] {
        let len: Int32 = try readInt(&buf)
        var seq = [String]()
        seq.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            try seq.append(FfiConverterString.read(from: &buf))
        }
        return seq
    }
}

private struct FfiConverterSequenceTypeGeographicCoordinate: FfiConverterRustBuffer {
    typealias SwiftType = [GeographicCoordinate]

    public static func write(_ value: [GeographicCoordinate], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for item in value {
            FfiConverterTypeGeographicCoordinate.write(item, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [GeographicCoordinate] {
        let len: Int32 = try readInt(&buf)
        var seq = [GeographicCoordinate]()
        seq.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            try seq.append(FfiConverterTypeGeographicCoordinate.read(from: &buf))
        }
        return seq
    }
}

private struct FfiConverterSequenceTypeLaneInfo: FfiConverterRustBuffer {
    typealias SwiftType = [LaneInfo]

    public static func write(_ value: [LaneInfo], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for item in value {
            FfiConverterTypeLaneInfo.write(item, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [LaneInfo] {
        let len: Int32 = try readInt(&buf)
        var seq = [LaneInfo]()
        seq.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            try seq.append(FfiConverterTypeLaneInfo.read(from: &buf))
        }
        return seq
    }
}

private struct FfiConverterSequenceTypeRoute: FfiConverterRustBuffer {
    typealias SwiftType = [Route]

    public static func write(_ value: [Route], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for item in value {
            FfiConverterTypeRoute.write(item, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [Route] {
        let len: Int32 = try readInt(&buf)
        var seq = [Route]()
        seq.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            try seq.append(FfiConverterTypeRoute.read(from: &buf))
        }
        return seq
    }
}

private struct FfiConverterSequenceTypeRouteStep: FfiConverterRustBuffer {
    typealias SwiftType = [RouteStep]

    public static func write(_ value: [RouteStep], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for item in value {
            FfiConverterTypeRouteStep.write(item, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [RouteStep] {
        let len: Int32 = try readInt(&buf)
        var seq = [RouteStep]()
        seq.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            try seq.append(FfiConverterTypeRouteStep.read(from: &buf))
        }
        return seq
    }
}

private struct FfiConverterSequenceTypeSpokenInstruction: FfiConverterRustBuffer {
    typealias SwiftType = [SpokenInstruction]

    public static func write(_ value: [SpokenInstruction], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for item in value {
            FfiConverterTypeSpokenInstruction.write(item, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [SpokenInstruction] {
        let len: Int32 = try readInt(&buf)
        var seq = [SpokenInstruction]()
        seq.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            try seq.append(FfiConverterTypeSpokenInstruction.read(from: &buf))
        }
        return seq
    }
}

private struct FfiConverterSequenceTypeVisualInstruction: FfiConverterRustBuffer {
    typealias SwiftType = [VisualInstruction]

    public static func write(_ value: [VisualInstruction], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for item in value {
            FfiConverterTypeVisualInstruction.write(item, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [VisualInstruction] {
        let len: Int32 = try readInt(&buf)
        var seq = [VisualInstruction]()
        seq.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            try seq.append(FfiConverterTypeVisualInstruction.read(from: &buf))
        }
        return seq
    }
}

private struct FfiConverterSequenceTypeWaypoint: FfiConverterRustBuffer {
    typealias SwiftType = [Waypoint]

    public static func write(_ value: [Waypoint], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for item in value {
            FfiConverterTypeWaypoint.write(item, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [Waypoint] {
        let len: Int32 = try readInt(&buf)
        var seq = [Waypoint]()
        seq.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            try seq.append(FfiConverterTypeWaypoint.read(from: &buf))
        }
        return seq
    }
}

private struct FfiConverterDictionaryStringString: FfiConverterRustBuffer {
    public static func write(_ value: [String: String], into buf: inout [UInt8]) {
        let len = Int32(value.count)
        writeInt(&buf, len)
        for (key, value) in value {
            FfiConverterString.write(key, into: &buf)
            FfiConverterString.write(value, into: &buf)
        }
    }

    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> [String: String] {
        let len: Int32 = try readInt(&buf)
        var dict = [String: String]()
        dict.reserveCapacity(Int(len))
        for _ in 0 ..< len {
            let key = try FfiConverterString.read(from: &buf)
            let value = try FfiConverterString.read(from: &buf)
            dict[key] = value
        }
        return dict
    }
}

/**
 * Typealias from the type name used in the UDL file to the custom type.  This
 * is needed because the UDL type name is used in function/method signatures.
 */
public typealias Uuid = UUID

public struct FfiConverterTypeUuid: FfiConverter {
    public static func read(from buf: inout (data: Data, offset: Data.Index)) throws -> Uuid {
        let builtinValue = try FfiConverterString.read(from: &buf)
        return UUID(uuidString: builtinValue)!
    }

    public static func write(_ value: Uuid, into buf: inout [UInt8]) {
        let builtinValue = value.uuidString
        return FfiConverterString.write(builtinValue, into: &buf)
    }

    public static func lift(_ value: RustBuffer) throws -> Uuid {
        let builtinValue = try FfiConverterString.lift(value)
        return UUID(uuidString: builtinValue)!
    }

    public static func lower(_ value: Uuid) -> RustBuffer {
        let builtinValue = value.uuidString
        return FfiConverterString.lower(builtinValue)
    }
}

public func FfiConverterTypeUuid_lift(_ value: RustBuffer) throws -> Uuid {
    try FfiConverterTypeUuid.lift(value)
}

public func FfiConverterTypeUuid_lower(_ value: Uuid) -> RustBuffer {
    FfiConverterTypeUuid.lower(value)
}

/**
 * Returns the next simulation state based on the desired strategy.
 * Results of this can be thought of like a stream from a generator function.
 *
 * This function is intended to be called once/second.
 * However, the caller may vary speed to purposefully replay at a faster rate
 * (ex: calling 3x per second will be a triple speed simulation).
 *
 * When there are now more locations to visit, returns the same state forever.
 */
public func advanceLocationSimulation(state: LocationSimulationState) -> LocationSimulationState {
    try! FfiConverterTypeLocationSimulationState.lift(try! rustCall {
        uniffi_ferrostar_fn_func_advance_location_simulation(
            FfiConverterTypeLocationSimulationState.lower(state), $0
        )
    })
}

/**
 * Creates a [`RouteResponseParser`] capable of parsing OSRM responses.
 *
 * This response parser is designed to be fairly flexible,
 * supporting both vanilla OSRM and enhanced Valhalla (ex: from Stadia Maps and Mapbox) outputs
 * which contain richer information like banners and voice instructions for navigation.
 */
public func createOsrmResponseParser(polylinePrecision: UInt32) -> RouteResponseParser {
    try! FfiConverterTypeRouteResponseParser.lift(try! rustCall {
        uniffi_ferrostar_fn_func_create_osrm_response_parser(
            FfiConverterUInt32.lower(polylinePrecision), $0
        )
    })
}

/**
 * Creates a [`Route`] from OSRM data.
 *
 * This uses the same logic as the [`OsrmResponseParser`] and is designed to be fairly flexible,
 * supporting both vanilla OSRM and enhanced Valhalla (ex: from Stadia Maps and Mapbox) outputs
 * which contain richer information like banners and voice instructions for navigation.
 */
public func createRouteFromOsrm(routeData: Data, waypointData: Data, polylinePrecision: UInt32) throws -> Route {
    try FfiConverterTypeRoute.lift(rustCallWithError(FfiConverterTypeParsingError.lift) {
        uniffi_ferrostar_fn_func_create_route_from_osrm(
            FfiConverterData.lower(routeData),
            FfiConverterData.lower(waypointData),
            FfiConverterUInt32.lower(polylinePrecision), $0
        )
    })
}

/**
 * Creates a [`RouteRequestGenerator`]
 * which generates requests to an arbitrary Valhalla server (using the OSRM response format).
 *
 * This is provided as a convenience for use from foreign code when creating your own [`routing_adapters::RouteAdapter`].
 */
public func createValhallaRequestGenerator(endpointUrl: String, profile: String,
                                           optionsJson: String?) throws -> RouteRequestGenerator
{
    try FfiConverterTypeRouteRequestGenerator.lift(rustCallWithError(FfiConverterTypeInstantiationError.lift) {
        uniffi_ferrostar_fn_func_create_valhalla_request_generator(
            FfiConverterString.lower(endpointUrl),
            FfiConverterString.lower(profile),
            FfiConverterOptionString.lower(optionsJson), $0
        )
    })
}

/**
 * Helper function for getting the route as an encoded polyline.
 *
 * Mostly used for debugging.
 */
public func getRoutePolyline(route: Route, precision: UInt32) throws -> String {
    try FfiConverterString.lift(rustCallWithError(FfiConverterTypeModelError.lift) {
        uniffi_ferrostar_fn_func_get_route_polyline(
            FfiConverterTypeRoute.lower(route),
            FfiConverterUInt32.lower(precision), $0
        )
    })
}

/**
 * Creates a location simulation from a set of coordinates.
 *
 * Optionally resamples the input line so that there is a maximum distance between points.
 */
public func locationSimulationFromCoordinates(coordinates: [GeographicCoordinate],
                                              resampleDistance: Double?) throws -> LocationSimulationState
{
    try FfiConverterTypeLocationSimulationState.lift(rustCallWithError(FfiConverterTypeSimulationError.lift) {
        uniffi_ferrostar_fn_func_location_simulation_from_coordinates(
            FfiConverterSequenceTypeGeographicCoordinate.lower(coordinates),
            FfiConverterOptionDouble.lower(resampleDistance), $0
        )
    })
}

/**
 * Creates a location simulation from a polyline.
 *
 * Optionally resamples the input line so that there is no more than the specified maximum distance between points.
 */
public func locationSimulationFromPolyline(polyline: String, precision: UInt32,
                                           resampleDistance: Double?) throws -> LocationSimulationState
{
    try FfiConverterTypeLocationSimulationState.lift(rustCallWithError(FfiConverterTypeSimulationError.lift) {
        uniffi_ferrostar_fn_func_location_simulation_from_polyline(
            FfiConverterString.lower(polyline),
            FfiConverterUInt32.lower(precision),
            FfiConverterOptionDouble.lower(resampleDistance), $0
        )
    })
}

/**
 * Creates a location simulation from a route.
 *
 * Optionally resamples the route geometry so that there is no more than the specified maximum distance between points.
 */
public func locationSimulationFromRoute(route: Route, resampleDistance: Double?) throws -> LocationSimulationState {
    try FfiConverterTypeLocationSimulationState.lift(rustCallWithError(FfiConverterTypeSimulationError.lift) {
        uniffi_ferrostar_fn_func_location_simulation_from_route(
            FfiConverterTypeRoute.lower(route),
            FfiConverterOptionDouble.lower(resampleDistance), $0
        )
    })
}

private enum InitializationResult {
    case ok
    case contractVersionMismatch
    case apiChecksumMismatch
}

// Use a global variable to perform the versioning checks. Swift ensures that
// the code inside is only computed once.
private var initializationResult: InitializationResult = {
    // Get the bindings contract version from our ComponentInterface
    let bindings_contract_version = 26
    // Get the scaffolding contract version by calling the into the dylib
    let scaffolding_contract_version = ffi_ferrostar_uniffi_contract_version()
    if bindings_contract_version != scaffolding_contract_version {
        return InitializationResult.contractVersionMismatch
    }
    if uniffi_ferrostar_checksum_func_advance_location_simulation() != 26307 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_func_create_osrm_response_parser() != 16550 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_func_create_route_from_osrm() != 42270 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_func_create_valhalla_request_generator() != 16275 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_func_get_route_polyline() != 31480 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_func_location_simulation_from_coordinates() != 30262 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_func_location_simulation_from_polyline() != 12234 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_func_location_simulation_from_route() != 47899 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_method_navigationcontroller_advance_to_next_step() != 3820 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_method_navigationcontroller_get_initial_state() != 63862 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_method_navigationcontroller_update_user_location() != 3165 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_method_routeadapter_generate_request() != 59034 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_method_routeadapter_parse_response() != 34481 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_method_routedeviationdetector_check_route_deviation() != 50476 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_method_routerequestgenerator_generate_request() != 63458 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_method_routeresponseparser_parse_response() != 44735 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_constructor_navigationcontroller_new() != 60881 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_constructor_routeadapter_new() != 32290 {
        return InitializationResult.apiChecksumMismatch
    }
    if uniffi_ferrostar_checksum_constructor_routeadapter_new_valhalla_http() != 3524 {
        return InitializationResult.apiChecksumMismatch
    }

    uniffiCallbackInitRouteDeviationDetector()
    uniffiCallbackInitRouteRequestGenerator()
    uniffiCallbackInitRouteResponseParser()
    return InitializationResult.ok
}()

private func uniffiEnsureInitialized() {
    switch initializationResult {
    case .ok:
        break
    case .contractVersionMismatch:
        fatalError("UniFFI contract version mismatch: try cleaning and rebuilding your project")
    case .apiChecksumMismatch:
        fatalError("UniFFI API checksum mismatch: try cleaning and rebuilding your project")
    }
}

// swiftlint:enable all
