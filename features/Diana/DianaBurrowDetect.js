import settings from "../../settings";
import { registerWhen } from "../../utils/variables";

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
            print("Start");
            // einfach x, y, z abfragen für koordianten villeicht methode creatBurrowWaypoints(x, y, z, type)
            break;
        case ("CRIT"):
            print("Mob");
            break;
        case ("FOOTSTEP"):
            print("Treasure");
            break;
    }
}

