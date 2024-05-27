import settings from "../../settings";
import { BOLD, AQUA, YELLOW} from "../../utils/constants";
import { registerWhen } from "../../utils/variables";
import { getGuiOpen, newOverlay } from "../../utils/overlays";
import { UIWrappedText } from "../../../Elementa";

let bobberOverlayObj = newOverlay("bobberOverlay", "bobberCounter", "bobbercounterExample", "render", "BobberLoc");
let bobberOverlay = bobberOverlayObj.overlay;

let bobberCount = 0;
let bobberText = new UIWrappedText(`${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`);
bobberText.setHeight((10).pixels())
bobberOverlay.addChild(bobberText);
const EntityFishHook = Java.type("net.minecraft.entity.projectile.EntityFishHook");

registerWhen(register('step', () => {
    bobberCount = World.getAllEntitiesOfType(EntityFishHook).filter(dist => dist.distanceTo(Player.getPlayer()) < 31).length
    if(getGuiOpen()) return;
    if(!bobberOverlay.children.includes(bobberText)) {
        bobberOverlay.clearChildren();
        bobberOverlay.addChild(bobberText);
    }
    bobberText.setText(`${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`);
}).setFps(1), () => settings.bobberCounter);
