let closestWarp = getClosestWarp(Math.round(Player.getLastX()), Math.round(Player.getLastY()), Math.round(Player.getLastZ()));
const inquisWarpKey = new KeyBind("Iqnuis Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
inquisWarpKey.registerKeyPress(() => {
    print(closestWarp);
    ChatLib.command(closestWarp);
});

function getClosestWarp(x,y,z){
    let warps = {
        castle: {x: -250, y: 130, z: 45},
        da: {x: 92, y: 75, z: 174},
        hub: {x: -3, y: 70, z: 70},
        museum: {x: -76, y: 76, z: 81}
    };

    let closestWarp;
    let closestDistance = Infinity;
    
    for (let warp in warps) {
        let distance = Math.sqrt(
            (warps[warp].x - x)**2 +
            (warps[warp].y - y)**2 +
            (warps[warp].z - z)**2
        );

        if (distance < closestDistance) {
            closestDistance = distance;
            closestWarp = warp;
        }
    }
    closestWarp = "warp " + closestWarp;
    return closestWarp;
}