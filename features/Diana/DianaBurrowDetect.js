import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { creatBurrowWaypoints } from "../general/Waypoints";

registerWhen(register("spawnParticle", (particle, type, event) => {
    // burrowDetect(particle, type);
    burrowDetectSoopy(particle, type);
}), () => settings.dianaBurrowDetect);

let burrialData = {
  points: [],
  locations: [],
  historicalLocations: [],
};
let potentialParticleLocs = {};
function burrowDetect(particle, type) {
    const particlepos = particle.getPos();
    const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
    const [x, y , z] = [xyz[0], xyz[1], xyz[2]];
    const typename = type.toString();
    switch (typename) {
        case ("CRIT_MAGIC"):
            creatBurrowWaypoints("Start", x, y, z);
            break;
        case ("CRIT"):
            creatBurrowWaypoints("Mob", x, y, z);
            break;
        case ("FOOTSTEP"):
            creatBurrowWaypoints("Treasure", x, y, z);
            break;
    }
}

function burrowDetectSoopy(particle, type) {
    let foundEnchant = false;
    let foundCrit = false;
    let foundStep = false;
    let isMob = undefined;
    if (
      particle.toString().startsWith("EntityEnchantmentTableParticleFX, ")
    ) {
      foundEnchant = true;
    } else if (particle.toString().startsWith("EntityCrit2FX, ")) {
      foundCrit = true;

      isMob = particle.getUnderlyingEntity().func_70534_d() > 0.5;
    } else if (particle.toString().startsWith("EntityFootStepFX, ")) {
      foundStep = true;
    } else if (particle.toString().startsWith("EntityCritFX, ")) {
      let locstr =
        Math.floor(particle.getX()) +
        "," +
        Math.floor(particle.getY() - 1) +
        "," +
        Math.floor(particle.getZ());

      let removed = false;
      burrialData.locations.filter((loc, i) => {
        if (!loc.clicked && loc.x + "," + loc.y + "," + loc.z === locstr) {
          loc.clicked = true;
          removed = true;
        }
      });
      print(removed);
      if (!removed) return;
      burrialData.locations = burrialData.locations.filter((a) => {
        if (!a.clicked) return true;
        if (
          calculateDistanceQuick(
            [a.x, a.y, a.z],
            [Player.getX(), Player.getY(), Player.getZ()]
          ) <
          15 * 15
        )
          return true;

        burrialData.historicalLocations.unshift(a);

        return false;
      });
      if (burrialData.historicalLocations.length > 10)
        burrialData.historicalLocations.pop();
      return;
    }
    if (!foundEnchant && !foundCrit && !foundStep) return;
    if (Math.abs(particle.getY() % 1) > 0.1) return;
    if (Math.abs(particle.getX() % 1) < 0.1) return;
    if (Math.abs(particle.getX() % 1) > 0.9) return;
    if (Math.abs(particle.getZ() % 1) < 0.1) return;
    if (Math.abs(particle.getZ() % 1) > 0.9) return;

    let locstr =
      Math.floor(particle.getX()) +
      "," +
      Math.floor(particle.getY() - 1) +
      "," +
      Math.floor(particle.getZ());
    let locarr = [
      Math.floor(particle.getX()),
      Math.floor(particle.getY() - 1),
      Math.floor(particle.getZ()),
    ];

    let found = false;

    burrialData.locations.forEach((loc) => {
      if (loc.x + "," + loc.y + "," + loc.z === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
      if (loc.x + 1 + "," + loc.y + "," + loc.z === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
      if (loc.x + 1 + "," + (loc.y + 1) + "," + loc.z === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
      if (loc.x + 1 + "," + (loc.y - 1) + "," + loc.z === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
      if (loc.x - 1 + "," + (loc.y + 1) + "," + loc.z === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
      if (loc.x - 1 + "," + (loc.y - 1) + "," + loc.z === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
      if (loc.x - 1 + "," + loc.y + "," + loc.z === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
      if (loc.x + "," + loc.y + "," + (loc.z + 1) === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
      if (loc.x + "," + loc.y + "," + (loc.z - 1) === locstr) {
        found = loc;
        loc.lastPing = Date.now();
      }
    });
    if (burrialData.historicalLocations) {
      burrialData.historicalLocations.forEach((loc) => {
        if (loc.x + "," + loc.y + "," + loc.z === locstr) {
          found = loc;
        }
    });
    if (
      !potentialParticleLocs[locstr] ||
      Date.now() - potentialParticleLocs[locstr].timestamp > 30000
    )
      this.potentialParticleLocs[locstr] = {
        enchant: 0,
        crit: 0,
        step: 0,
        isMob: 0,
        timestamp: Date.now(),
      };

    if (foundEnchant) potentialParticleLocs[locstr].enchant++;
    if (foundCrit) potentialParticleLocs[locstr].crit++;
    if (foundStep) potentialParticleLocs[locstr].step++;
    if (foundCrit && isMob) potentialParticleLocs[locstr].isMob++;
    if (foundCrit && !isMob) potentialParticleLocs[locstr].isMob--;

    potentialParticleLocs[locstr].timestamp = Date.now();

    if (
      potentialParticleLocs[locstr].enchant >= 1 &&
      potentialParticleLocs[locstr].step >= 2
    ) {
      if (found) {
        found.type =
          potentialParticleLocs[locstr].isMob >= 1
            ? 1
            : potentialParticleLocs[locstr].crit >
              potentialParticleLocs[locstr].enchant / 20
            ? 0
            : 2;
        return;
      }
      burrialData.locations.push({
        x: locarr[0],
        y: locarr[1],
        z: locarr[2],
        type:
          potentialParticleLocs[locstr].isMob >= 1
            ? 1
            : potentialParticleLocs[locstr].crit >
              potentialParticleLocs[locstr].enchant / 20
            ? 0
            : 2,
        tier: -1,
        chain: -1,
        fromApi: false,
      });
      // gib burrialdata.locations aus
      for (let i = 0; i < burrialData.locations.length; i++) {
        print(burrialData.locations[i].x);
        print(burrialData.locations[i].y);
        print(burrialData.locations[i].z);
        print(burrialData.locations[i].type);
      }

    }
  }
}

function calculateDistanceQuick(a, b) {
    return (
        (a[0] - b[0])**2 +
        (a[1] - b[1])**2 +
        (a[2] - b[2])**2
    );
  }