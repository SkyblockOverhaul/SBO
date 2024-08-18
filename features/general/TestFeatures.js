let lastTimeThrown = 0
let throwBool = false
registerWhen(register("playerInteract", (action, pos) => {
    let item = Player.getHeldItem()
    if (item.getLore()[1].includes("§8Lava Rod")) {
        lastTimeThrown = Date.now()
        ChatLib.chat("&6[SBO] &eYou have thrown your Lava Rod") 
        throwBool = true
    }
}), () => Settings.testFeatures);

registerWhen(register("tick", () => {
    // if older than 2min and 30sec send message
    if (Date.now() - lastTimeThrown > 150000 && throwBool) {
        ChatLib.chat("&6[SBO] &eYou have not thrown your Lava Rod in over 2 minutes and 30 seconds")
        // play multiple sounds
        for (let i = 0; i < 3; i++) {
            setTimeout(() => {
                World.playSound("random.levelup", 100, 0.5*i);
            }, i * 500);
        }
        throwBool = false
    }
}), () => Settings.testFeatures);


registerWhen(register("soundPlay", (pos, name, volume, pitch, categoryName, event) => {
    // printDev(`Sound: ${name} | Volume: ${volume} | Pitch: ${pitch} | Category: ${categoryName}`)
    if (name == "mob.ghast.scream" || name == "mob.ghast.charge") {
        ChatLib.chat("sound for rag axe " + name)
        Client.showTitle("RAG AXE", "", 0, 90, 20);
    }
}), () => Settings.testFeatures);

// dojo sounds:
// [DEV]: Sound: mob.cat.hiss | Volume: 2 | Pitch: 1.4920635223388672 | Category: ANIMALS
// [DEV]: Sound: mob.zombie.woodbreak | Volume: 1.5 | Pitch: 1 | Category: MOBS

// register("chat", (message, event) => {
//     message = message.removeFormatting();
//     if (!message.includes("Powder") && !message.includes("Refelctor") && !message.includes("Blue Goblin Egg") && !message.includes("Heart")) {
//         cancel(event);
//     }
//     if (message.includes("Refelctor")) {
//         Client.showTitle("&9Robotron Reflector", "&eCarrot", 0, 40, 20);
//     }
//     if (message.includes("Blue Goblin Egg")) {
//         Client.showTitle("&3Blue Goblin Egg", "&eCarrot", 0, 40, 20);
//     }
// }).setCriteria("&r&aYou received ${message}");
