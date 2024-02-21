import settings from "../../settings";
import { loadGuiSettings, saveGuiSettings } from "../../utils/functions";


guiSettings = loadGuiSettings();

let bobberCount = 0;
let message = true;


var movecounter = new Gui();
const EntityFishHook = Java.type("net.minecraft.entity.projectile.EntityFishHook")

register("dragged", (dx, dy, x, y) => {
    if (!movecounter.isOpen()) return
    guiSettings["BobberLoc"]["x"] = x
    guiSettings["BobberLoc"]["y"] = y
    saveGuiSettings(guiSettings)
    ChatLib.chat("SavedGuiSettings")
});

register("command", () => {
    movecounter.open()
}).setName("movebobbercounter");



register('step', () => {
    bobberCount = World.getAllEntitiesOfType(EntityFishHook).filter(dist => dist.distanceTo(Player.getPlayer()) < 31).length
    
    if (bobberCount > settings.bobberwarning && message && settings.bobberpc && settings.bobberCounter) {
        ChatLib.command(`pc ${bobberCount} bobbers!`)
        message = false
    }
    else if (bobberCount <= settings.bobberwarning && !message) {
        ChatLib.command(`pc Less than ${settings.bobberwarning+1} bobbers!`)
        message = true
    }

}).setFps(5)




register('renderOverlay', () => {
    if (settings.bobberCounter) {
        const color = settings.bobberColor
        Renderer.colorize(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
        if (settings.bobberChroma) {
            bobberCount.toString();
            if (bobberCount > 9) {
                Renderer.drawString(`§zBobbers: ${bobberCount}`,  guiSettings["BobberLoc"]["x"], guiSettings["BobberLoc"]["y"], true)
            } else {
                Renderer.drawString(`Bobbers: ${bobberCount}`,  guiSettings["BobberLoc"]["x"], guiSettings["BobberLoc"]["y"], true)
            }
        } else {
            Renderer.drawString(`Bobbers: ${bobberCount}`,  guiSettings["BobberLoc"]["x"], guiSettings["BobberLoc"]["y"], true)
        }
    }
    if (movecounter.isOpen()) {
        const color = settings.bobberColor
        Renderer.drawStringWithShadow(`x: ${Math.round( guiSettings["BobberLoc"]["x"])}, y: ${Math.round(guiSettings["BobberLoc"]["y"])}`, parseInt( guiSettings["BobberLoc"]["x"]) - 65, parseInt(guiSettings["BobberLoc"]["y"]) - 12)
        Renderer.colorize(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
        Renderer.drawString(`Bobbers:`,  guiSettings["BobberLoc"]["x"], guiSettings["BobberLoc"]["y"])
    }
})