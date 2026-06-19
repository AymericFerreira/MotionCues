# Motion Cues — keep rules.
# The app uses no reflection-heavy libraries; defaults plus these are sufficient.

# Keep the foreground service, tile service and activity entry points referenced from the manifest.
-keep class io.github.aymericferreira.motioncues.overlay.OverlayService { *; }
-keep class io.github.aymericferreira.motioncues.tile.MotionCuesTileService { *; }
-keep class io.github.aymericferreira.motioncues.MainActivity { *; }
