import SwiftUI

///
public protocol FerrostarTheme {
    var banner: InstructionRowTheme { get }
    var bannerListRow: InstructionRowTheme { get }
    var bannerOffsetRow: InstructionRowTheme? { get }
}

public struct DefaultFerrostarTheme: FerrostarTheme {
    public let banner: any InstructionRowTheme = DefaultInstructionRowTheme()
    public let bannerListRow: any InstructionRowTheme = DefaultInstructionRowTheme()
    public let bannerOffsetRow: (any InstructionRowTheme)? = nil
}
