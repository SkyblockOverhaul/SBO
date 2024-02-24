import settings from "../settings.js";

register("spawnParticle", (particle, type, event) => {
    burrowDetect(particle);
});


function burrowDetect(particle) {
    const type = particle.toString();
}