package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import java.awt.Color

object Burrows : Category("Burrows") {
    override val description = "Finding burrows: the guesses, burrow detection and chains."

    init {
        sub {
            this.title = "Burrow Detection"
            this.description = "You should keep all of these On for the optimal experience. Without the Arrow Guess, your rates will be much worse, and without the Close Burrow Detection your burrows will not turn into Mob/Treasure/Start burrows when getting close and holding a spade, and it will also make your experience and rates worse."
        }
    }

    var spadeGuess by boolean(true) {
        this.name = Literal("Spade Guess")
        this.description = Literal("Guess the burrow location when using spade ability. Needs Dripping Lava Particles and set /particlequality to Extreme for more accuracy.")
    }

    var curveSpadeGuess by boolean(true) {
        this.name = Literal("Curve Spade Guess (WIP)")
        this.since = "0.7.2-beta.2"
        this.description = Literal("Guesses the exact block from the spade trail's curve, from the second particle on. Turn off to use the old spade guess if it acts up.")
        depends(::spadeGuess)
    }

    var arrowGuess by boolean(true) {
        this.name = Literal("Arrow Guess")
        this.description = Literal("Guesses the burrow location from the arrow direction after digging a burrow. \n" +
            "§cHave Dust and Smoke Particles enabled and /particlequality extreme!\n" +
            "§aDo every burrow you see and Use Spade when the mod tells you to for doing Diana the fastest way!"
        )
    }

    var closeBurrowDetection by boolean(true) {
        this.name = Literal("Close Burrow Detection")
        this.description = Literal("Detects burrow locations when being close to them from the particles when holding shovel to register/update it as a Treasure, Mob or Start burrow. Needs Critical Hit and Enchant particles, and set /particlequality extreme. To reset waypoints type /sboclearburrows.")
    }

    init {
        sub {
            this.title = "Guesses"
        }
    }

    var showArrowSubGuesses by boolean(false) {
        this.name = Literal("Show Arrow Sub Guesses")
        this.description = Literal("Displays the other possible arrow guess locations as gray (configurable) waypoints until the active guess advances. This is not recommended for inexperienced users to reduce confusion and is so defaults to being disabled.")
    }

    var showTextOnSubGuess by boolean(false) {
        this.name = Literal("Show Text On Sub Guess")
        depends(::showArrowSubGuesses)
        this.description = Literal("Shows \"Possible\" text on sub guesses in addition to the block higlight. Might increase visual clutter in exchange of making subguesses more visible.")
    }

    var showTitleWhenFailure by boolean(false) {
        this.name = Literal("Show Title When Failure")
        this.description = Literal("Shows a title to use spade when the arrow guess fails to solve the burrow. This might sometimes show wrongfully on a second solve attempt if the first was successfull and second failed for any reason.")
    }

    var showTitleWhenChainEnds by boolean(true) {
        this.name = Literal("Show Title When Chain Ends")
        this.description = Literal("Shows a title to use spade when the burrow chain is complete and there's no more guesses or burrows at least 90 blocks nearby, which will usually point to a new Start burrow to hold up your concurrent chains.")
    }

    init {
        sub {
            this.title = "Chains"
        }
    }

    var highlightChainEnds by boolean(true) {
        this.name = Literal("Highlight Chain Ends")
        this.since = "0.7.2-beta.2"
        this.description = Literal("Gives the last burrow of a chain its own color, also while it is still a spade or arrow guess. Known once you dig the burrow before it.")
    }

    var ChainEndColor by color(
        Color(1.0f, 0.84f, 0.0f).rgb
    ) {
        this.name = Literal("Chain End Color")
        this.since = "0.7.2-beta.2"
        depends(::highlightChainEnds)
        this.description = Literal("Color of the last burrow of a chain, also while it is a guess.")
        this.allowAlpha = false
    }

    var showChainStep by boolean(false) {
        this.name = Literal("Show Chain Step")
        this.since = "0.7.2-beta.2"
        this.description = Literal("Shows which burrow of the chain a spade or arrow guess is, like 3/10. Known once you dig the burrow before it.")
    }

    var ongoingChainsDisplay by boolean(false) {
        this.name = Literal("Ongoing Chains Display")
        this.description = Literal("Shows a display on screen with active chains amount. You can get up to 7 chains running at the same time.")
    }

    var simpleChainsOverlayMode by boolean(true) {
        this.name = Literal("Simple Chains Overlay Mode")
        depends(::ongoingChainsDisplay)
        this.description = Literal("Only shows Pending Start and Active waypoint amount, never showing internal state mismatch issues aimed at users that want to troubleshoot/debug mod issues.")
    }

    init {
        sub {
            this.title = "Spade Guess Optimizer"
        }
    }

    var spadeGuessOptimizer by boolean(false) {
        this.name = Literal("Spade Guess Optimizer")
        this.since = "0.7.2-beta.3"
        this.description = Literal("When you finish a chain, it shows a location within your specified distance from your last burrow to stand and use spade for new chain discovery. Works with any amount of chains you have. The guess line to spade guess locations is prioritized over any other burrow type. Spade guess locations can be 5 blocks further than your set limit per chain you lose.")
    }

    var optimizerDetour by int(35) {
        this.range = 10..150
        this.slider = true
        this.name = Literal("Optimizer Detour")
        this.since = "0.7.2-beta.3"
        depends(::spadeGuessOptimizer)
        this.description = Literal("How many blocks away from the chain's last burrow you're willing to go for a place to use your spade.")
    }

    var lookForMaxChains by boolean(true) {
        this.name = Literal("Look For Max Chains")
        this.since = "0.7.2-beta.3"
        depends(::spadeGuessOptimizer)
        this.description = Literal("After starting your game, a lobby swap or /sboclearburrows, shows places across the hub to use your spade until you discover 7 chains. The guess line and warp key go to them during the process until 3 spade guesses in a row fail to discover a new chain, which then the feature stops and redirects you to your guess burrows.")
    }

    var SpadeGuessOptimizerColor by color(
        Color(1.0f, 0.65f, 0.0f).rgb
    ) {
        this.name = Literal("Spade Guess Optimizer Color")
        this.since = "0.7.2-beta.3"
        depends(::spadeGuessOptimizer)
        this.description = Literal("Color of the place to stand and use your spade to find a new chain.")
        this.allowAlpha = false
    }

    init {
        sub {
            this.title = "Spade"
        }
    }

    var muteBuggedSpadeSounds by boolean(true) {
        this.name = Literal("Mute Bugged Spade Sounds")
        this.description = Literal("Fixes an Hypixel issue where using the Ancestral, Archaic or the Deific spade sometimes would play a bugged Minecraft music that does not end and could be scary for people. This does the exact same thing SkyHanni option under the same name does, which is to cancel the bugged music from playing, until Hypixel fixes the issue server-side.")
    }
}
