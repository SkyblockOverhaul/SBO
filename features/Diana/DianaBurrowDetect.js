import settings from "../settings.js";

register("spawnParticle", (particle, type, event) => {
    
});


function burrowDetect() {
    let burrow = getBurrow();
    if (burrow !== undefined) {
        let formatted = [];
        formatWaypoints([burrow], 0, 1, 0);
        renderWaypoint(formatted);
    }
}