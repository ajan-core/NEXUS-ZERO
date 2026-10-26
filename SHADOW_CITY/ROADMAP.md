# SHADOW CITY — build roadmap

## Current prototype files
- `godot/project.godot`: Godot 4 project configuration, mobile-friendly Compatibility renderer.
- `godot/main.tscn`: project entry scene.
- `godot/main.gd`: procedurally generated neon city block, third-person hero, camera follow, keyboard/touch action setup, enemies, attack/XP/level HUD.

## Planned next work
1. Validate project in a real Godot 4 editor and fix any parse/runtime issues.
2. Improve touch UI layout for different phone aspect ratios; add a proper virtual joystick and attack/dash controls.
3. Add character rig/animations and original art assets.
4. Add enemy hit points/attack behavior, health/damage, mission goals and save data.
5. Configure Android export templates, export APK, then install-test on a real phone.

## Status honesty
This is source-code scaffolding for a 3D prototype, not yet a verified runnable build or APK. The project must be opened/tested in Godot before claiming build success. It runs locally/offline once exported; no online service is required for the prototype.
