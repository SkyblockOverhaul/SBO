import settings from "../../settings";

register("chat", (event) =>{
    if (!settings.jacobHider) return;
    cancel(event);
}).setCriteria("[NPC] Jacob: ").setContains()

register("chat", (event) =>{
    if (!settings.hideRadioWeak) return;
    cancel(event);
}).setCriteria("&r&7Your radio is weak. Find another enjoyer to boost it.&r")