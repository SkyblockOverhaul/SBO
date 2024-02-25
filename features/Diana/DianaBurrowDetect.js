import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { creatBurrowWaypoints } from "../general/Waypoints";

registerWhen(register("spawnParticle", (particle, type, event) => {
    burrowDetect(particle, type);
    //burrowDetectSoopy(particle, type);
}), () => settings.dianaBurrowDetect);


let burrows = [];
function burrowDetect(particle, type) {
    const particlepos = particle.getPos();
    const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
    const [x, y , z] = [xyz[0], xyz[1], xyz[2]];
    const typename = type.toString();
    switch (typename) {
        // case ("CRIT_MAGIC"):
        //     calcCoords(particle);
        //     // creatBurrowWaypoints("Start", x, y, z);
        //     break;
        // case ("CRIT"):
        //     // creatBurrowWaypoints("Mob", x, y, z);
        //     break;
        // case ("FOOTSTEP"):
        //     // creatBurrowWaypoints("Treasure", x, y, z);
        //     break;
        case ("FOOTSTEP"): // Loads burrow waypoints by footstep
            xyz.unshift("Treasure");

            closest = getClosest(xyz, burrows);
            if (closest[1] > 3) {
                burrows.push(xyz);
                
            }
        
            break;
        // Determine burrow type
        case ("CRIT_MAGIC"):
            xyz.unshift("Start");
            closest = getClosest(xyz, burrows);
            if (closest[1] < 3)
                closest[0][0] = "Start";
                creatBurrowWaypoints("Start", x, y, z);
            break;
        case ("CRIT"):
            xyz.unshift("Mob");
            closest = getClosest(xyz, burrows);
            if (closest[1] < 3)
                closest[0][0] = "Mob";
                creatBurrowWaypoints("Mob", x, y, z);
            break;
    }
}

function getClosest(origin, positions) {
    let closestPosition = positions.length > 0 ? positions[0] : [0, 0, 0];
    let closestDistance = 999;
    let distance = 999;

    positions.forEach(position => {
        distance = Math.hypot(origin[1] - position[1], origin[2] - position[2], origin[3] - position[3]);
        if (distance < closestDistance) {
            closestDistance = distance;
            closestPosition = position;
        }
    });

    return [closestPosition, closestDistance];
};


// function calculateDistanceQuick(a, b) {
//     return (
//         (a[0] - b[0])**2 +
//         (a[1] - b[1])**2 +
//         (a[2] - b[2])**2
//     );
//   }


