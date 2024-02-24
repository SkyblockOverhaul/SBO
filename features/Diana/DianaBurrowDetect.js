import settings from "../../settings.js";

register("step", (particle, type, event) => {
    burrowDetect(particle);
}).setFps(1);


function burrowDetect(particle, type) {
    const particlepos = particle.getPos();
    const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
    const [x, y, z] = [xyz[0], xyz[1], xyz[2]];
    const type = particle.toString();
    print(type);
    switch (type) {
        case "EntityCrit2FX":
            print("Crit");
            break;
    }
}