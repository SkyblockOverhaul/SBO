import { convertToPascalCase } from "../../utils/functions";
import { data } from "../../utils/variables";

let entityList = [];
let entities = [];
export function getEntities() { return entities };

function testClass(entity) {
    try {
        mob = Java.type(entity).class;
        World.getAllEntitiesOfType(mob);
        entityList.push(mob);
        return true;
    } catch(err) {
        return false;
    }
}
export function updateEntityList() {
    entityList = [];

    data.moblist.forEach(mob => {
        const PascalCaseMob = convertToPascalCase(mob);
        if (PascalCaseMob === "OtherPlayerMP" && testClass(`net.minecraft.client.entity.Entity${PascalCaseMob}`)) return;
        else if (testClass(`net.minecraft.entity.monster.Entity${PascalCaseMob}`)) return;
        else if (testClass(`net.minecraft.entity.boss.Entity${PascalCaseMob}`)) return;
        else if (testClass(`net.minecraft.entity.passive.Entity${PascalCaseMob}`)) return;
    });
}
updateEntityList();

register("tick", () =>{
    entities = [];
    entityList.forEach(entityClass => {
        // Match coloring
        const colorMap = {
            "monster": [1, 0, 0], // Red
            "boss": [0, 1, 0],    // Green
            "passive": [0, 0, 1], // Blue
        };
        const color = colorMap[Object.keys(colorMap).find(type => entityClass.toString().includes(type))] || [1, 1, 1];

        // Add entities
        const filteredEntities = data.y == 0 ? World.getAllEntitiesOfType(entityClass) :
            World.getAllEntitiesOfType(entityClass).filter(entity => Math.abs(Player.getY() - entity.getY()) < data.y);
        if (filteredEntities.length === 0) return;
        entities.push([[...filteredEntities], color]);
    });
});
