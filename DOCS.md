# SkyblockOverhaul Developer Documentation

This document covers the technical details of SBO's internal systems.

---

## Table of Contents

- [Event System](#event-system)
  - [@SboEvent Annotation](#sboevent-annotation)
  - [Event Categories](#event-categories)
  - [Creating Custom Events](#creating-custom-events)
  - [Manual Registration](#manual-registration)
- [Helper Functions](#helper-functions)
  - [Register](#register)
  - [Chat](#chat)
  - [World](#world)
  - [Player](#player)
  - [HTTP](#http)
- [Settings](#settings)
- [Mixins](#mixins)

---

## Event System

SBO uses a custom event bus built on Fabric events. It provides compile-time registration via KSP.

### @SboEvent Annotation

The `@SboEvent` annotation automatically registers event listeners at compile time.

```kotlin
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.ChatMessageEvent

object MyFeature {
    @SboEvent
    fun onChatMessage(event: ChatMessageEvent) {
        // Handle the event
    }
}
```

#### Priority

Control execution order with priority:

```kotlin
@SboEvent(SboEvent.Priority.HIGH)   // Runs first
@SboEvent(SboEvent.Priority.NORMAL) // Default, runs in middle
@SboEvent(SboEvent.Priority.LOW)    // Runs last
```

#### Requirements

- Must be inside an `object` or `companion object`
- Must have exactly one parameter (the event type)
- Event type must be from `net.sbo.mod.utils.events.impl.*`

---

### Event Categories

| Package | Description | Available Events |
|---------|-------------|------------------|
| `impl.entity` | Entity-related events | `DianaMobDeathEvent`, `EntityLoadEvent`, `EntityUnloadEvent`, `EntitiyHitEvent` |
| `impl.game` | Game state events | `ChatMessageEvent`, `ChatMessageAllowEvent`, `DisconnectEvent`, `GameCloseEvent`, `InventorySlotUpdateEvent`, `TickEvent`, `WorldChangeEvent`, `SentCommandEvent`, `PlayerInteractEvent`, `SentMessageEvent` |
| `impl.guis` | GUI events | `GuiCloseEvent`, `GuiKeyEvent`, `GuiMouseClickAfter`, `GuiMouseClickBefore`, `GuiOpenEvent`, `GuiRenderEvent`, `GuiPostRenderEvent` |
| `impl.packets` | Network packet events | `PacketReceiveEvent`, `PacketSendEvent` |
| `impl.partyfinder` | Party finder events | `PartyFinderOpenEvent`, `PartyFinderRefreshListEvent` |
| `impl.diana` | Diana-specific events | `BurrowDugEvent` |

---

### Creating Custom Events

#### 1. Define the Event Class

```kotlin
package net.sbo.mod.utils.events.impl.game

data class MyCustomEvent(
    val playerName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

#### 2. Emit the Event

```kotlin
import net.sbo.mod.utils.events.SBOEvent

// Somewhere in your code
SBOEvent.emit(MyCustomEvent("Player123", "Hello world!"))
```

#### 3. Listen to the Event

```kotlin
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.MyCustomEvent

object MyListener {
    @SboEvent
    fun onMyCustomEvent(event: MyCustomEvent) {
        println("${event.playerName}: ${event.message}")
    }
}
```

---

### Manual Registration

If you can't use `@SboEvent` (e.g., in dynamic contexts or lambdas):

```kotlin
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.events.annotations.SboEvent

// Register with class
val unregister = SBOEvent.on(MyEvent::class) { event ->
    println("Got event!")
}

// Or use inline reified version
val unregister = SBOEvent.on<MyEvent> { event ->
    println("Got event!")
}

// Unregister when done
unregister()
```

---

## Helper Functions

### Register

Located: `net.sbo.mod.utils.events.Register`

#### Register.command

Create chat commands:

```kotlin
// Simple command
Register.command("mycommand") { args ->
    println("Command executed!")
}

// With aliases
Register.command("mycommand", "myalias", "mc") { args ->
    // args contains any arguments after the command
    if (args.isNotEmpty()) {
        println("First arg: ${args[0]}")
    }
}

// With required player context
Register.command("sbocheck", "sboc") { args ->
    if (!World.isInSkyblock()) {
        Chat.chat("§cYou must be in Skyblock!")
        return@Register.command
    }
    // ...
}
```

#### Register.onTick

Run code every N ticks:

```kotlin
// Run every 20 ticks (1 second)
Register.onTick(20) { unregister ->
    println("Ticking...")
    // Return true to unregister
    if (someCondition) {
        unregister()
    }
    false // Keep running
}
```

#### Register.onChatMessage

Listen to chat messages:

```kotlin
import java.util.regex.Pattern

// Listen to messages matching a regex
Register.onChatMessage(Pattern.compile(".*Diana.*")) { message, match ->
    println("Matched: ${message.string}")
}
```

#### Register.onChatMessageCancelable

Listen and potentially cancel messages:

```kotlin
Register.onChatMessageCancelable(Pattern.compile(".*spammy.*")) { message, match ->
    // Return true to cancel the message
    true
}
```

---

### Chat

Located: `net.sbo.mod.utils.chat.Chat`

```kotlin
import net.sbo.mod.utils.chat.Chat

// Send message to player
Chat.chat("Hello!")

// Send formatted message (supports color codes)
Chat.chat("§6This is gold text!")

// Send message to party
Chat.say("Message to party")

// Send system message
Chat.sendMessage(Component.literal("System message"))
```

---

### World

Located: `net.sbo.mod.utils.game.World`

```kotlin
import net.sbo.mod.utils.game.World

// Check if in Skyblock
if (World.isInSkyblock()) {
    // ...
}

// Get current world name
val worldName = World.getWorld()

// Check location
val inHub = World.getWorld() == "hub"
```

---

### Player

Located: `net.sbo.mod.utils.Player`

```kotlin
import net.sbo.mod.utils.Player

// Get last known position
val pos = Player.getLastPosition()
val x = pos.x
val y = pos.y
val z = pos.z

// Get player object
val player = Player.mc.player
```

---

### Helper

Located: `net.sbo.mod.utils.Helper`

Helper contains various utility functions:

```kotlin
import net.sbo.mod.utils.Helper

// Get purse
val purse = Helper.getPurse()

// Read player inventory
val inventory = Helper.readPlayerInv()

// Format numbers
val formatted = Helper.formatNumber(1000000, true) // "1M"

// Get server tps
val tps = Helper.getTPS()
```

---

### HTTP

Located: `net.sbo.mod.utils.http.Http`

```kotlin
import net.sbo.mod.utils.http.Http

// GET request
Http.sendGetRequest("https://api.example.com/data")
    .result { response ->
        val body = response.body?.string
        println(body)
    }
    .error { exception ->
        println("Error: ${exception.message}")
    }

// POST request
Http.sendPostRequest("https://api.example.com/post", "application/json", "{}")
    .result { response ->
        println("Status: ${response.code}")
    }
```

---

## Settings

Settings are defined using ResourcefulConfig. See `net.sbo.mod.settings.categories` for examples.

### Basic Setting

```kotlin
object MySettings : CategoryKt("My Settings") {
    var myBoolean by boolean(false) {
        this.name = Literal("My Boolean")
        this.description = Literal("Description here")
    }

    var myInt by int(10) {
        this.range = 0..100
        this.slider = true
        this.name = Literal("My Integer")
    }

    var myString by strings("default") {
        this.name = Literal("My String")
    }
}
```

### Register in Settings

In `Settings.kt`:

```kotlin
category(MySettings)
```

---

## Mixins

The mod uses Mixins for Minecraft hooks. Mixin configs are in `src/main/resources/`.

### Adding a New Mixin

1. Create the mixin class in the appropriate package:
   ```kotlin
   package net.sbo.mod.mixin;

   @Mixin(Gui.class)
   public class MyMixin {
       // ...
   }
   ```

2. Register in `sbo.mixins.json`:
   ```json
   "MyMixin"
   ```

---

## Common Patterns

### Feature Toggle Pattern

```kotlin
object MyFeature {
    @SboEvent
    fun onTick(event: TickEvent) {
        if (!Settings.myFeatureEnabled) return
        // Feature code
    }
}
```

### GUI State Management

```kotlin
object MyGUIState {
    var instance: MyGUI? = null

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        instance = event.screen as? MyGUI
    }

    @SboEvent
    fun onGuiClose(event: GuiCloseEvent) {
        instance = null
    }
}
```

### Cleanup on Disconnect

```kotlin
object MyFeature {
    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        // Clean up any state
        MyFeatureState.reset()
    }
}
```