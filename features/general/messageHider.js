import settings from "../../settings";

register("chat", (event) =>{
    if (!settings.jacobHider) return;
    cancel(event);
}).setCriteria("[NPC] Jacob: ").setContains()