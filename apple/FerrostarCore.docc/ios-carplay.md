# CarPlay on iOS

CarPlay is currently a work in progress that you can see in the demo app.

- [Apple - Using the CarPlay Simulator](https://developer.apple.com/documentation/carplay/using-the-carplay-simulator)
- By definition, your application specifies a `CPTemplateApplicationSceneDelegate` in its Info.plist. This instance will have an instance of `FerrostarCarPlayManager`. The Demo application references this via the `UISceneSession.userInfo`. `FerrostarCarPlayManager` will have a reference to the singleton `FerrostarCore` from your iOS application.
