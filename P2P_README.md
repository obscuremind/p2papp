# Streambox P2P Add-on — Build Notes

## If you see: `Unsupported class file major version 65`
That's Gradle running on JDK 21. Set **Gradle JDK = 17** in Android Studio (or upgrade the Gradle wrapper to 8.6+).

## Run signaling
```bash
npm i
node server.js
```
Emulator uses `ws://10.0.2.2:8081`. For a physical device, change to your LAN IP inside `buildP2PFactory()`.
