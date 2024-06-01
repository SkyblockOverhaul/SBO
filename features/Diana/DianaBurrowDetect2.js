
const { EvictingQueue } = require('google-collections');
const { UMatrixStack, Skytils, Utils, RenderUtil, ItemUtil, SBInfo, SkyblockIsland } = require('skytils-utils');
const {
    ClientChatReceivedEvent,
    RenderWorldLastEvent,
    WorldEvent,
    EventPriority,
    SubscribeEvent,
    TickEvent,
    MainReceivePacketEvent,
    PacketEvent
} = require('minecraft-forge');

const { BlockPos, Vec3i, AxisAlignedBB } = require('minecraft-utils');
const { Color } = require('java.awt');
const { GlStateManager, Blocks, ItemStack, EnumParticleTypes, S2APacketParticles, C07PacketPlayerDigging, C08PacketPlayerBlockPlacement } = require('minecraft');

class GriffinBurrows {
    constructor() {
        this.particleBurrows = new Map();
        this.lastDugParticleBurrow = null;
        this.recentlyDugParticleBurrows = new EvictingQueue(5);
        this.hasSpadeInHotbar = false;
    }

    @SubscribeEvent
    onTick(event) {
        if (event.phase !== TickEvent.Phase.START) return;
        this.hasSpadeInHotbar = mc.thePlayer !== null && Utils.inSkyblock && Array.from({ length: 8 }).some((_, i) => mc.thePlayer.inventory.getStackInSlot(i).isSpade);
    }

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.HIGHEST)
    onChat(event) {
        if (event.type === 2) return;
        const unformatted = event.message.unformattedText.stripControlCodes();
        if (Skytils.config.showGriffinBurrows &&
            (unformatted.startsWith('You died') || unformatted.startsWith('☠ You were killed') ||
                unformatted.startsWith('You dug out a Griffin Burrow! (') ||
                unformatted === 'You finished the Griffin burrow chain! (4/4)')
        ) {
            if (this.lastDugParticleBurrow !== null) {
                const particleBurrow = this.particleBurrows.get(this.lastDugParticleBurrow);
                if (!particleBurrow) return;
                this.recentlyDugParticleBurrows.add(this.lastDugParticleBurrow);
                this.particleBurrows.delete(particleBurrow.blockPos);
                this.printDevMessage(`Removed ${particleBurrow}`, 'griffin');
                this.lastDugParticleBurrow = null;
            }
        }
    }

    @SubscribeEvent
    onSendPacket(event) {
        if (!Utils.inSkyblock || !Skytils.config.showGriffinBurrows || mc.theWorld === null || mc.thePlayer === null) return;
        const pos = (() => {
            if (event.packet instanceof C07PacketPlayerDigging && event.packet.status === C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                return event.packet.position;
            }
            if (event.packet instanceof C08PacketPlayerBlockPlacement && event.packet.stack !== null) {
                return event.packet.position;
            }
            return null;
        })();

        if (!pos) return;
        if (mc.thePlayer.heldItem?.isSpade !== true || mc.theWorld.getBlockState(pos).block !== Blocks.grass) return;
        const particleBurrow = this.particleBurrows.get(pos);
        if (particleBurrow) {
            this.printDevMessage(`Clicked on ${particleBurrow.blockPos}`, 'griffin');
            this.lastDugParticleBurrow = particleBurrow.blockPos;
        }
    }

    @SubscribeEvent
    onWorldRender(event) {
        if (Skytils.config.showGriffinBurrows) {
            const matrixStack = new UMatrixStack();
            for (const pb of this.particleBurrows.values()) {
                if (pb.hasEnchant && pb.hasFootstep && pb.type !== -1) {
                    pb.drawWaypoint(event.partialTicks, matrixStack);
                }
            }
        }
    }

    @SubscribeEvent
    onWorldChange(event) {
        this.particleBurrows.clear();
        this.recentlyDugParticleBurrows.clear();
    }

    @SubscribeEvent
    onReceivePacket(event) {
        if (!Utils.inSkyblock) return;
        if (Skytils.config.showGriffinBurrows && this.hasSpadeInHotbar && event.packet instanceof S2APacketParticles) {
            if (SBInfo.mode !== SkyblockIsland.Hub.mode) return;
            const { x, y, z, type, count, speed, xOffset, yOffset, zOffset } = event.packet;
            const particleType = ParticleType.getParticleType(event.packet);
            if (!particleType) return;
            const pos = new BlockPos(x, y, z).down();
            if (this.recentlyDugParticleBurrows.contains(pos)) return;
            const burrow = this.particleBurrows.get(pos) || new ParticleBurrow(pos, false, false, -1);
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
            this.particleBurrows.set(pos, burrow);
        }
    }

    printDevMessage(message, category) {
        console.log(`[${category}] ${message}`);
    }
}

class Diggable {
    constructor(x, y, z, type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.blockPos = new BlockPos(x, y, z);
    }

    get waypointText() {
        switch (this.type) {
            case 0: return '§aStart (Particle)';
            case 1: return '§cMob (Particle)';
            case 2: return '§6Treasure (Particle)';
            default: return 'Burrow (Particle)';
        }
    }

    get color() {
        switch (this.type) {
            case 0: return Skytils.config.emptyBurrowColor;
            case 1: return Skytils.config.mobBurrowColor;
            case 2: return Skytils.config.treasureBurrowColor;
            default: return Color.WHITE;
        }
    }

    drawWaypoint(partialTicks, matrixStack) {
        const { viewerX, viewerY, viewerZ } = RenderUtil.getViewerPos(partialTicks);
        const pos = this.blockPos;
        const x = pos.x - viewerX;
        const y = pos.y - viewerY;
        const z = pos.z - viewerZ;
        const distSq = x * x + y * y + z * z;
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        RenderUtil.drawFilledBoundingBox(
            matrixStack,
            new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).expandBlock(),
            this.color,
            Math.max(0.1 + 0.005 * distSq, 0.2)
        );
        GlStateManager.disableTexture2D();
        if (distSq > 5 * 5) RenderUtil.renderBeaconBeam(x, y + 1, z, this.color.getRGB(), 1.0, partialTicks);
        RenderUtil.renderWaypointText(
            this.waypointText,
            pos.x + 0.5,
            pos.y + 5.0,
            pos.z + 0.5,
            partialTicks,
            matrixStack
        );
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
    }
}

class ParticleBurrow extends Diggable {
    constructor(x, y, z, hasFootstep, hasEnchant, type) {
        super(x, y, z, type);
        this.hasFootstep = hasFootstep;
        this.hasEnchant = hasEnchant;
    }

    static fromVec3(vec3, hasFootstep, hasEnchant, type) {
        return new ParticleBurrow(vec3.x, vec3.y, vec3.z, hasFootstep, hasEnchant, type);
    }
}

Object.defineProperty(ItemStack.prototype, 'isSpade', {
    get() {
        return ItemUtil.getSkyBlockItemID(this) === 'ANCESTRAL_SPADE';
    }
});

const ParticleType = Object.freeze({
    EMPTY: packet => packet.type === EnumParticleTypes.CRIT_MAGIC && packet.count === 4 && packet.speed === 0.01 && packet.xOffset === 0.5 && packet.yOffset === 0.1 && packet.zOffset === 0.5,
    MOB: packet => packet.type === EnumParticleTypes.CRIT && packet.count === 3 && packet.speed === 0.01 && packet.xOffset === 0.5 && packet.yOffset === 0.1 && packet.zOffset === 0.5,
    TREASURE: packet => packet.type === EnumParticleTypes.DRIP_LAVA && packet.count === 2 && packet.speed === 0.01 && packet.xOffset === 0.35 && packet.yOffset === 0.1 && packet.zOffset === 0.35,
    FOOTSTEP: packet => packet.type === EnumParticleTypes.FOOTSTEP && packet.count === 1 && packet.speed === 0.0 && packet.xOffset === 0.05 && packet.yOffset === 0.0 && packet.zOffset === 0.05,
    ENCHANT: packet => packet.type === EnumParticleTypes.ENCHANTMENT_TABLE && packet.count === 5 && packet.speed === 0.05 && packet.xOffset === 0.5 && packet.yOffset === 0.4 && packet.zOffset === 0.5,

    getParticleType(packet) {
        if (!packet.isLongDistance) return null;
        for (const type in ParticleType) {
            if (ParticleType[type](packet)) {
                return type;
            }
        }
        return null;
    }
});

module.exports = { GriffinBurrows, Diggable, ParticleBurrow, ParticleType };
