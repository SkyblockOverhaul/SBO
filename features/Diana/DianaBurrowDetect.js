import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { creatBurrowWaypoints, getBurrowWaypoints } from "../general/Waypoints";

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
            break;
        case ("CRIT"):
            xyz.unshift("Mob");
            closest = getClosest(xyz, burrows);
            if (closest[1] < 3)
                closest[0][0] = "Mob";
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

register("step", () => {
    burrows.forEach(([type, x, y, z]) => {
        creatBurrowWaypoints(type, x, y, z);
    });
}).setFps(10);

register("HitBlock", () => {
    block = Player.lookingAt()
    let [type,x, y, z] = block.toString().replace("x=","").replace("y=","").replace("z=","").replace("}","").split(",");
    x = parseInt(x);
    y = parseInt(y);
    z = parseInt(z);
    const surroundingCoordinates = getSurroundingCoordinates(x, y, z);
    console.log(surroundingCoordinates);
    // create foreach sourrounding coordinates vurrowwaypoint
    for (let i = 0; i < surroundingCoordinates.length; i++) {
        creatBurrowWaypoints("Start", surroundingCoordinates[i][0], surroundingCoordinates[i][1], surroundingCoordinates[i][2]);
    }
});

function removeBurrow(x, y, z) {
    let burrowwaypoints = getBurrowWaypoints();
    for (let i = 0; i < burrowwaypoints.length; i++) {
        if (burrowwaypoints[0] == x && burrowwaypoints[1] == y && burrowwaypoints[2] == z) {
            burrowwaypoints.splice(1);
        }
    }
    burrows = burrows.filter(([_, bx, by, bz]) => bx !== x || by !== y || bz !== z);
}
function getSurroundingCoordinates(x, y, z) {
    const surroundingCoordinates = [];
    for (let i = x - 1; i <= x + 1; i++) {
        for (let k = z - 1; k <= z + 1; k++) {
            surroundingCoordinates.push([i, y+1, k]);
        }
    }
    return surroundingCoordinates;
}

// Call the function with the desired x, y, z coordinates to remove the burrow



