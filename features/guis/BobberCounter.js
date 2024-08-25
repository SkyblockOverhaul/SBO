import settings from "../../settings";
import { BOLD, AQUA, YELLOW} from "../../utils/constants";
import { registerWhen } from "../../utils/variables";
import { OverlayTextLine, SboOverlay} from "../../utils/overlays";

let bobberOverlay = new SboOverlay("bobberOverlay", "bobberOverlay", "render", "BobberLoc");
let bobberOverlayText = new OverlayTextLine("");

let bobberCount = 0;

const EntityFishHook = Java.type("net.minecraft.entity.projectile.EntityFishHook");

registerWhen(register('step', () => {
    bobberCount = World.getAllEntitiesOfType(EntityFishHook).filter(dist => dist.distanceTo(Player.getPlayer()) < 31).length
    bobberOverlay.setLines([bobberOverlayText.setText(`${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`)]);
}).setFps(1), () => settings.bobberOverlay);
