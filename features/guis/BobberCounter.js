import settings from "../../settings";
import { loadGuiSettings, saveGuiSettings } from "../../utils/functions";
import { BOLD, AQUA, YELLOW} from "../../utils/constants";
import { registerWhen } from "../../utils/variables";
import {
    UIBlock,
    UIWrappedText,
    ChildBasedRangeConstraint
} from "../../../Elementa";

let bobberGuiSettings = loadGuiSettings();
let loadedBobber = false;
const Color = Java.type("java.awt.Color");
export let bobberOverlaySelected = false;
export let bobberOverlay = new UIBlock(new Color(0.2, 0.2, 0.2, 0));
const dragOffset = {x: 0, y: 0};

bobberOverlay.setWidth(new ChildBasedRangeConstraint());
bobberOverlay.setHeight(new ChildBasedRangeConstraint());
bobberOverlay.onMouseClick((comp, event) => {
    bobberOverlaySelected = true;
    print("Bobber overlay selected");
    dragOffset.x = event.absoluteX;
    dragOffset.y = event.absoluteY;
});

bobberOverlay.onMouseRelease(() => {
    bobberOverlaySelected = false;
});

bobberOverlay.onMouseDrag((comp, mx, my) => {
    if(!bobberOverlaySelected) return;
    bobberGuiSettings = loadGuiSettings();
    const absoluteX = mx + comp.getLeft()
    const absoluteY = my + comp.getTop()
    const dx = absoluteX - dragOffset.x;
    const dy = absoluteY - dragOffset.y;
    dragOffset.x = absoluteX;
    dragOffset.y = absoluteY;
    const newX = bobberOverlay.getLeft() + dx;
    const newY = bobberOverlay.getTop() + dy;
    bobberOverlay.setX(newX.pixels());
    bobberOverlay.setY(newY.pixels());
    bobberGuiSettings["BobberLoc"]["x"] = newX;
    bobberGuiSettings["BobberLoc"]["y"] = newY;
    saveGuiSettings(bobberGuiSettings);
});

let bobberCount = 0;
let bobberText = new UIWrappedText(`${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`);
bobberText.setWidth((51).pixels())
bobberText.setHeight((9).pixels())
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
    bobberText.setText(`${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`);
}).setFps(1), () => settings.bobberCounter);
