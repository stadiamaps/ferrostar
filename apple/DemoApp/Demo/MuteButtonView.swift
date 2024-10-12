//
//  MuteButton.swift
//  Ferrostar Demo
//
//  Created by Marek Sabol on 08/10/2024.
//

import SwiftUI

struct MuteButton: View {
    @Binding var isMuted: Bool

    var body: some View {
        Button(action: {
            isMuted.toggle()
        }) {
            Image(systemName: isMuted ? "speaker.slash.fill" : "speaker.2.fill")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 18, height: 18)
                .padding()
                .foregroundColor(.black)
                .background(Color.white)
                .clipShape(Circle())
                .shadow(radius: 10)
        }
        .padding(.trailing, 18) // Right
        .padding(.top, 112)
    }
}

#Preview {
    MuteButton(isMuted: .constant(false))
}
