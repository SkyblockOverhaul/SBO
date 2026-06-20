# Contributing to SkyblockOverhaul

Thanks for your interest in contributing! This document will help you get started with development.

## Prerequisites

- **Gradle** (included via Gradle Wrapper)
- **IntelliJ IDEA** (recommended) with Kotlin plugin
- **Git**

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/SkyblockOverhaul/SBO-Kotlin.git
cd SBO-Kotlin
```

### 2. Open in IntelliJ IDEA

1. Open the project in IntelliJ IDEA
2. Wait for Gradle to sync and download dependencies

### 3. Run the Mod

```bash
./gradlew runClient
```

Or use the **Run** configuration that Gradle generates in IntelliJ.

### 4. Build the Mod

```bash
# Build for both versions
./gradlew build

# Build for specific version
./gradlew :1.21.11-fabric:build
./gradlew :26.1.2-fabric:build
```

Built JARs will be in `build/versions/`.

## Project Structure

```
SBO-Kotlin/
├── src/
│   └── main/
│       └── kotlin/
│           └── net/sbo/mod/
│               ├── SBOKotlin.kt          # Main mod entry point
│               ├── diana/                 # Diana event logic
│               │   ├── burrows/           # Burrow detection/guessing
│               │   ├── achievements/      # Achievement system
│               │   └── ...
│               ├── general/               # General commands
│               ├── guis/                  # GUI screens
│               ├── overlays/              # On-screen overlays
│               ├── partyfinder/           # Party finder feature
│               ├── settings/              # Config categories
│               │   └── categories/       # Individual setting groups
│               └── utils/                 # Utilities
│                   ├── waypoint/         # Waypoint management
│                   ├── render/           # Rendering helpers
│                   ├── chat/             # Chat utilities
│                   └── ...
├── event-processor/                       # Event processing annotation processor
├── build.gradle.kts                       # Build configuration
└── gradle.properties                     # Version and dependency versions
```

## Adding New Features

### 1. Add a Setting

Settings are defined in `src/main/kotlin/net/sbo/mod/settings/categories/`. Create or extend a category:

```kotlin
object MyCategory : CategoryKt("My Category") {
    var mySetting by boolean(true) {
        this.name = Literal("My Setting")
        this.description = Literal("Description of my setting")
    }
}
```

Don't forget to register the category in `Settings.kt`:

```kotlin
category(MyCategory)
```

### 2. Add a Command

Use the `Register.command()` helper:

```kotlin
Register.command("mycommand", "myalias") { args ->
    // Handle command
    // args contains any arguments passed after the command
}
```

### 3. Add Keybinds

Add in `SboKeyBinds.kt`:

```kotlin
val myKeyBind: KeyMapping = KeyMapping(
    "key.sbo-kotlin.my_key",
    InputConstants.Type.KEYSYM,
    GLFW.GLFW_KEY_X,  // Replace with your key
    SBO_CATEGORY
)

fun register() {
    // ... existing keybinds
    KeyBindingHelper.registerKeyBinding(myKeyBind)
}

// Then handle in the keybind listener
handlePressAction(myKeyBind) {
    // Action to perform
}
```

### 4. Add Overlay

1. Create a new file in `overlays/`
2. Extend from `UOverlay` (from Elementa)
3. Register in `OverlayManager.kt`

### 5. Using the Event System

The mod uses a custom event system. For detailed documentation on:
- `@SboEvent` annotation
- Creating and emitting custom events
- Event priorities
- Helper functions

See [DOCS.md](DOCS.md).

## Code Style

- **Language**: Kotlin
- **Indentation**: 4 spaces (not tabs)
- **Line endings**: LF (Unix-style)
- **Max line length**: 120 characters (soft guideline)

### Naming Conventions

- Use camelCase for variables and functions
- Use PascalCase for classes and types
- Use SCREAMING_SNAKE_CASE for constants
- Prefix boolean variables with `is`, `has`, `should`, etc.

## Pull Request Guidelines

1. **Target Branch**: Submit PRs to the `Diana-V2` branch. The `main` branch is not actively developed and only receives changes in rare cases. All development happens on `Diana-V2`.
2. **Branch naming**: Use descriptive branch names like `feature/new-tracker` or `fix/burrow-detection`
3. **Commit messages**: Use clear, concise messages describing what changed
4. **Testing**: Test your changes in-game before submitting
5. **PR Description**: Explain what your change does and why it's needed

### Before Submitting

- [ ] Code compiles without errors
- [ ] Mod runs without crashes
- [ ] New settings are properly documented
- [ ] New Features are documented in the [README.md](README.md)
- [ ] No debug print statements left in code

## Common Issues

### "Gradle sync failed"
Try:
```bash
./gradlew --refresh-dependencies
```

### "Missing Minecraft"
Make sure you've opened the project in IntelliJ so Gradle can download Minecraft.

## Resources

- [Fabric Docs](https://fabricmc.net/wiki/documentation:fabric_mod_json)
- [Elementa Docs](https://github.com/essentialgg/Elementa/tree/master/docs)

## License

By contributing, you agree that your code will be licensed under the license in the repository.