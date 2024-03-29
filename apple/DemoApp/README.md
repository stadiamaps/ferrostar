#  Ferrostar iOS Demo

This project is a minimal demonstration of how to use [Ferrostar](https://github.com/stadiamaps/ferrostar)
in an iOS application.

## Quickstart

1. Sign up for a [free Stadia Maps account](https://client.stadiamaps.com/signup/?utm_content=ferrostar_ios&utm_campaign=ferrostar_demos&utm_source=github)
2. Go through property setup and create an API key.
3. Create an `API-Keys.plist` file with your API key. Shown below opened as XML/Source: 

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>STADIAMAPS_API_KEY</key>
    <string>00000000-0000-0000-0000-000000000000</string>
</dict>
</plist>
```

4. Run the app!

NOTES:

* If you get some nonsensical build errors about FerrostarCore not being available, close Xcode and ONLY open this project (not the main Ferrostar Swift Package)
* At the moment, this is purely for functional testing; no effort has yet gone into polish
