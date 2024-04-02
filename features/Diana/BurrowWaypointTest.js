function calcBurrowWaypoint(particle, type, event) {
    let foundEnchant = false;
    let foundCrit = false;
    let foundStep = false;
    let isMob = undefined;

    if (particle.toString().startsWith("EntityEnchantmentTableParticleFX, ")) {
    foundEnchant = true;
    } 
    else if (particle.toString().startsWith("EntityCrit2FX, ")) {
    foundCrit = true;

    isMob = particle.getUnderlyingEntity().func_70534_d() > 0.5;
    } 
    else if (particle.toString().startsWith("EntityFootStepFX, ")) {
    foundStep = true;
    } 
    else if (particle.toString().startsWith("EntityCritFX, ")) {
        let locstr = Math.floor(particle.getX()) + "," + Math.floor(particle.getY() - 1) + "," + Math.floor(particle.getZ());

        let removed = false;
        this.burrialData.locations.filter((loc, i) => {
            if (!loc.clicked && loc.x + "," + loc.y + "," + loc.z === locstr) {
            loc.clicked = true;
            removed = true;
            }
        });
        if (!removed) return;
        this.burrialData.locations = this.burrialData.locations.filter((a) => {
            if (!a.clicked) return true;
            if (
            calculateDistanceQuick(
                [a.x, a.y, a.z],
                [Player.getX(), Player.getY(), Player.getZ()]
            ) <
            15 * 15
            )
            return true;

            this.burrialData.historicalLocations.unshift(a);

            return false;
        });
        if (this.burrialData.historicalLocations.length > 10)
            this.burrialData.historicalLocations.pop();

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

    this.burrialData.locations.forEach((loc) => {
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
    if (this.burrialData.historicalLocations) {
        this.burrialData.historicalLocations.forEach((loc) => {
            if (loc.x + "," + loc.y + "," + loc.z === locstr) {
            found = loc;
            }
        });
    }

    if (!this.potentialParticleLocs[locstr] || Date.now() - this.potentialParticleLocs[locstr].timestamp > 30000) {
        this.potentialParticleLocs[locstr] = {
            enchant: 0,
            crit: 0,
            step: 0,
            isMob: 0,
            timestamp: Date.now(),
        };
    }


    if (foundEnchant) this.potentialParticleLocs[locstr].enchant++;
    if (foundCrit) this.potentialParticleLocs[locstr].crit++;
    if (foundStep) this.potentialParticleLocs[locstr].step++;
    if (foundCrit && isMob) this.potentialParticleLocs[locstr].isMob++;
    if (foundCrit && !isMob) this.potentialParticleLocs[locstr].isMob--;

    this.potentialParticleLocs[locstr].timestamp = Date.now();

    if ( this.potentialParticleLocs[locstr].enchant >= 1 && this.potentialParticleLocs[locstr].step >= 2) {
        if (found) {
            found.type =
            this.potentialParticleLocs[locstr].isMob >= 1
                ? 1
                : this.potentialParticleLocs[locstr].crit >
                this.potentialParticleLocs[locstr].enchant / 20
                ? 0
                : 2;
            return;
        }
        this.burrialData.locations.push({
            x: locarr[0],
            y: locarr[1],
            z: locarr[2],
            type: this.potentialParticleLocs[locstr].isMob >= 1
                ? 1 : this.potentialParticleLocs[locstr].crit > this.potentialParticleLocs[locstr].enchant / 20
                ? 0 : 2, tier: -1, chain: -1, fromApi: false,
        });

        // World.playSound("note.pling", 100, 2);
        // print cords of found location
        print("Found location: " + locarr[0] + ", " + locarr[1] + ", " + locarr[2]);
    }
}

function calculateDistance(p1,p2){
    var a=p2[0]-p1[0];
    var b=p2[1]-p1[1];
    var c=p2[2]-p1[2];
    
    let ret=Math.hypot(a,b,c);
    
    if(ret<0){
        ret*=-1;
    }
    return ret;
}

function calculateDistanceQuick(p1,p2){
    var a=p2[0]-p1[0];
    var b=p2[1]-p1[1];
    var c=p2[2]-p1[2];
    
    let ret=a*a+b*b+c*c;
    
    if(ret<0){
        ret*=-1;
    }
    return ret;
}

register("spawnParticle", (particle, type, event) => {
    calcBurrowWaypoint(particle, type, event)
})