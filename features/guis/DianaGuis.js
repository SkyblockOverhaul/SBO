import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { Overlay } from "../../utils/overlay";
import { getWorld } from "../../utils/world";
import { getTracker, state } from "../../utils/functions";
dianaMobTrackerExample = 
`&4Diana Mob Kills
Minos Inquisitor: 
Minos Champion:
Minotaur:
Gaia Construct:
Siamese Lynx:
Minos Hunter:
`



const DianaMobTracker = new Overlay("dianaMobTracker",["Hub"], [150,150,1],"moveMobCoounter",dianaMobTrackerExample);
registerWhen(register("entityDeath", () => {
    state.entityDeathOccurred = true;
    setTimeout(() => {
        state.entityDeathOccurred = false;
    }, 1000);
    switch (settings.dianaMobTracker){
        case 1:
            DianaMobTracker.message =
            `&4Diana Mob Kills
Minos Inquisitor: 40
Minos Champion: 20
Minotaur: 30
Gaia Construct: 10
Siamese Lynx: 20 
Minos Hunter: 30
`
            break;
        case 2:
            DianaMobTracker.message =
            `Event View`
            break;
        case 3:
            DianaMobTracker.message =
            `Session View`
            break;
            
    }
}), () => getWorld() === "Hub" && settings.dianaMobTracker !== 0);



