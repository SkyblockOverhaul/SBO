import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getBurrowWaypoints } from "../general/Waypoints";

registerWhen(register("spawnParticle", (particle, type, event) => {
    burrowDetect(particle, type);
}), () => settings.dianaBurrowDetect);


function burrowDetect(particle, type) {
    const particlepos = particle.getPos();
    const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
    const [x, y , z] = [xyz[0], xyz[1], xyz[2]];
    const typename = type.toString();
    switch (typename) {
        case ("CRIT_MAGIC"):
            getBurrowWaypoints("Start", x, y, z);
            break;
        case ("CRIT"):
            getBurrowWaypoints("Mob", x, y, z);
            break;
        case ("FOOTSTEP"):
            getBurrowWaypoints("Treasure", x, y, z);
            break;
    }
}

