import settings from "../../settings";
import { loadGuiSettings } from "../../utils/functions";
import { BOLD, AQUA, YELLOW} from "../../utils/constants";
import { registerWhen } from "../../utils/variables";
import { getGuiOpen, newOverlay } from "../../utils/overlays";
import { UIWrappedText } from "../../../Elementa";

let bobberGuiSettings = loadGuiSettings();
let loadedBobber = false;
let bobberOverlayObj = newOverlay("bobberOverlay", settings.bobberCounter, "bobbercounterExample", "render", "BobberLoc");
let bobberOverlay = bobberOverlayObj.overlay;


let bobberCount = 0;
let bobberText = new UIWrappedText(`${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`);
bobberText.setHeight((10).pixels())
bobberOverlay.addChild(bobberText);
const EntityFishHook = Java.type("net.minecraft.entity.projectile.EntityFishHook");

function loadBobberOverlay() {
    if(bobberGuiSettings != undefined && !loadedBobber) {
        bobberOverlay.setX((bobberGuiSettings["BobberLoc"]["x"]).pixels());
        bobberOverlay.setY((bobberGuiSettings["BobberLoc"]["y"]).pixels());
        loadedBobber = true;
    }
}
loadBobberOverlay();

registerWhen(register('step', () => {
    bobberCount = World.getAllEntitiesOfType(EntityFishHook).filter(dist => dist.distanceTo(Player.getPlayer()) < 31).length
    if(getGuiOpen()) return;
    if(!bobberOverlay.children.includes(bobberText)) {
        bobberOverlay.clearChildren();
        bobberOverlay.addChild(bobberText);
    }
    bobberText.setText(`${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`);
}).setFps(1), () => settings.bobberCounter);
