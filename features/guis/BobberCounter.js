import settings from "../../settings";
import { loadGuiSettings, saveGuiSettings } from "../../utils/functions";
import { Overlay } from "../../utils/Overlay";
import { BOLD, GOLD, AQUA} from "../../utils/constants";
import { registerWhen } from "../../utils/variables";


guiSettings = loadGuiSettings();

bobbercounterExample =
`${GOLD}${BOLD}Bobber:
`
let BobberCounter = new Overlay("bobberCounter",["all"], [10, 10, 0],"moveBobberCounter",bobbercounterExample, "bobberCounter");

let bobberCount = 0;
let loadedBobber = false;


const EntityFishHook = Java.type("net.minecraft.entity.projectile.EntityFishHook")

function bobberOverlay() {
    if(settings.bobberCounter) {
        if(guiSettings != undefined && !loadedBobber) {
            BobberCounter.setX(guiSettings["BobberLoc"]["x"]);
            BobberCounter.setY(guiSettings["BobberLoc"]["y"]);
            BobberCounter.setScale(guiSettings["BobberLoc"]["s"]);
            loadedBobber = true;
        }
        if( guiSettings["BobberLoc"]["x"] != BobberCounter.X || guiSettings["BobberLoc"]["y"] != BobberCounter.Y || guiSettings["BobberLoc"]["s"] != BobberCounter.S)
        {
            guiSettings["BobberLoc"]["x"] = BobberCounter.X;
            guiSettings["BobberLoc"]["y"] = BobberCounter.Y;
            guiSettings["BobberLoc"]["s"] = BobberCounter.S;
            saveGuiSettings(guiSettings);
        }
        BobberCounter.message = `${GOLD}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`
    }
}

registerWhen(register('step', () => {
    bobberCount = World.getAllEntitiesOfType(EntityFishHook).filter(dist => dist.distanceTo(Player.getPlayer()) < 31).length
        bobberOverlay();
}).setFps(5), () => settings.bobberCounter);
