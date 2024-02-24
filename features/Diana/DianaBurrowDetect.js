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
            print(x);
            print(y);
            print(z);
            break;
        case ("CRIT"):
            print("Mob");
            print(x);
            print(y);
            print(z);
            break;
        case ("FOOTSTEP"):
            print("Treasure");
            print(x);
            print(y);
            print(z);
            break;
    }
}

