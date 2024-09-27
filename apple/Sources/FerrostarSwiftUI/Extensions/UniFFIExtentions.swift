//
//  UniFFIExtentions.swift
//  FerrostarCore
//
//  Created by Patrick Wolowicz on 27.09.24.
//

import FerrostarCoreFFI
import FerrostarCore

extension FerrostarCore {
    
    public var isNavigating: Bool {
        return self.state?.isNavigating ?? false
    }
}

extension NavigationState {
    public var isNavigating: Bool {
        if case .navigating = tripState {
            return true
        } else {
            return false
        }
    }
}
