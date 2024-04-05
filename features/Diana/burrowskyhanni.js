const GriffinBurrowParticleFinder = {
    config: SkyHanniMod.feature.event.diana,

    recentlyDugParticleBurrows: new TimeLimitedSet(1 * 60 * 1000), // 1 minute in milliseconds
    burrows: new Map(),
    lastDugParticleBurrow: null,
    fakeBurrow: null,

    onDebugDataCollect: function(event) {
        console.log("Griffin Burrow Particle Finder");

        if (!DianaAPI.isDoingDiana()) {
            console.log("not doing diana");
            return;
        }

        console.log("burrows: " + this.burrows.size);
        for (const burrow of this.burrows.values()) {
            const location = burrow.location;
            const found = burrow.found;
            console.log(location.printWithAccuracy(1));
            console.log("type: " + burrow.getType());
            console.log("found: " + found);
            console.log(" ");
        }
    },


    onChatPacket: function(event) {
        if (!this.isEnabled()) return;
        if (!config.burrowsSoopyGuess) return;
        const packet = event.packet;

        if (packet instanceof S2APacketParticles) {
            const particleType = ParticleType.getParticleType(packet);
            if (particleType !== null) {
                const location = packet.toLorenzVec().toBlockPos().down().toLorenzVec();
                if (this.recentlyDugParticleBurrows.contains(location)) return;
                const burrow = this.burrows.getOrPut(location, () => new Burrow(location));
                
                switch (particleType) {
                    case ParticleType.FOOTSTEP:
                        burrow.hasFootstep = true;
                        break;
                    case ParticleType.ENCHANT:
                        burrow.hasEnchant = true;
                        break;
                    case ParticleType.EMPTY:
                        burrow.type = 0;
                        break;
                    case ParticleType.MOB:
                        burrow.type = 1;
                        break;
                    case ParticleType.TREASURE:
                        burrow.type = 2;
                        break;
                }

                if (burrow.hasEnchant && burrow.hasFootstep && burrow.type !== -1) {
                    if (!burrow.found) {
                        new BurrowDetectEvent(burrow.location, burrow.getType()).postAndCatch();
                        burrow.found = true;
                    }
                }
            }
        }
    },

    ParticleType: {
        EMPTY: function(packet) {
            return packet.particleType === net.minecraft.util.EnumParticleTypes.CRIT_MAGIC &&
                packet.particleCount === 4 &&
                packet.particleSpeed === 0.01 &&
                packet.xOffset === 0.5 &&
                packet.yOffset === 0.1 &&
                packet.zOffset === 0.5;
        },
        MOB: function(packet) {
            return packet.particleType === net.minecraft.util.EnumParticleTypes.CRIT &&
                packet.particleCount === 3 &&
                packet.particleSpeed === 0.01 &&
                packet.xOffset === 0.5 &&
                packet.yOffset === 0.1 &&
                packet.zOffset === 0.5;
        },
        TREASURE: function(packet) {
            return packet.particleType === net.minecraft.util.EnumParticleTypes.DRIP_LAVA &&
                packet.particleCount === 2 &&
                packet.particleSpeed === 0.01 &&
                packet.xOffset === 0.35 &&
                packet.yOffset === 0.1 &&
                packet.zOffset === 0.35;
        },
        FOOTSTEP: function(packet) {
            return packet.particleType === net.minecraft.util.EnumParticleTypes.FOOTSTEP &&
                packet.particleCount === 1 &&
                packet.particleSpeed === 0.0 &&
                packet.xOffset === 0.05 &&
                packet.yOffset === 0.0 &&
                packet.zOffset === 0.05;
        },
        ENCHANT: function(packet) {
            return packet.particleType === net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE &&
                packet.particleCount === 5 &&
                packet.particleSpeed === 0.05 &&
                packet.xOffset === 0.5 &&
                packet.yOffset === 0.4 &&
                packet.zOffset === 0.5;
        },

        getParticleType: function(packet) {
            if (!packet.isLongDistance) return null;
            for (const type of Object.values(this)) {
                if (typeof type === "function" && type(packet)) {
                    return type;
                }
            }
            return null;
        }
    },

    onWorldChange: function(event) {
        this.reset();
    },

    reset: function() {
        this.burrows.clear();
        this.recentlyDugParticleBurrows.clear();
    },

    onChat: function(event) {
        if (!this.isEnabled()) return;
        if (!config.burrowsSoopyGuess) return;
        const message = event.message;
        if (message.startsWith("§eYou dug out a Griffin Burrow!") ||
            message === "§eYou finished the Griffin burrow chain! §r§7(4/4)"
        ) {
            BurrowAPI.lastBurrowRelatedChatMessage = SimpleTimeMark.now();
            const burrow = this.lastDugParticleBurrow;
            if (burrow !== null) {
                if (!this.tryDig(burrow)) {
                    this.fakeBurrow = burrow;
                }
            }
        }
        if (message === "§cDefeat all the burrow defenders in order to dig it!") {
            BurrowAPI.lastBurrowRelatedChatMessage = SimpleTimeMark.now();
        }
    },

    tryDig: function(location, ignoreFound = false) {
        const burrow = this.burrows.get(location);
        if (burrow === undefined) return false;
        if (!burrow.found && !ignoreFound) return false;
        this.burrows.delete(location);
        this.recentlyDugParticleBurrows.add(location);
        this.lastDugParticleBurrow = null;

        new BurrowDugEvent(burrow.location).postAndCatch();
        return true;
    },

    onBlockClick: function(event) {
        if (!this.isEnabled()) return;
        if (!config.burrowsSoopyGuess) return;

        const location = event.position;
        if (!event.itemInHand.isDianaSpade || location.getBlockAt() !== Blocks.grass) return;

        if (location === this.fakeBurrow) {
            this.fakeBurrow = null;
            this.tryDig(location, true); // ignoreFound = true
            return;
        }

        if (this.burrows.has(location)) {
            this.lastDugParticleBurrow = location;

            DelayedRun.runDelayed(() => {
                if (BurrowAPI.lastBurrowRelatedChatMessage.passedSince() > 2 * 1000) {
                    this.burrows.delete(location);
                }
            }, 1000); // 1 second in milliseconds
        }
    },

    isEnabled: function() {
        return DianaAPI.isDoingDiana();
    }
};

class Burrow {
    constructor(location, hasFootstep = false, hasEnchant = false, type = -1, found = false) {
        this.location = location;
        this.hasFootstep = hasFootstep;
        this.hasEnchant = hasEnchant;
        this.type = type;
        this.found = found;
    }

    getType() {
        switch (this.type) {
            case 0:
                return BurrowType.START;
            case 1:
                return BurrowType.MOB;
            case 2:
                return BurrowType.TREASURE;
            default:
                return BurrowType.UNKNOWN;
        }
    }
}

function isEnabled() {
    return DianaAPI.isDoingDiana();
}
