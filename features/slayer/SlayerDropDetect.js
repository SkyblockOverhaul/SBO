// import settings from "../../settings";
// import { registerWhen } from "../../utils/variables";
// import { isDataLoaded } from "../../utils/checkData";
// import { isInSkyblock } from '../../utils/functions';

// let dropArray = undefined;



// registerWhen(register("chat", (trash, type, drop) => {
//     if (isDataLoaded() && isInSkyblock()) {
//         let magicFindMatch = drop.match(/\+(&r&b)?(\d+)%/);
//         let magicFind = magicFindMatch ? magicFindMatch[2] + '%' : null;
        
//         let amountMatch = drop.match(/&r&f&r&7(\d+)x/);
//         let amount = amountMatch ? amountMatch[1] : '1';
        
//         let dropNameMatch = drop.match(/&r&f(.+)&r&b/);
//         let dropName = dropNameMatch ? dropNameMatch[1].trim() : null;
        
//         dropArray = { magicFind, amount, dropName };
//     }
// }).setCriteria("${trash}&l${type} RARE DROP! ${drop}"), () => settings.slayerDropDetect);
// //${trash}&l${type} RARE DROP! &r&7(&r${amount} &r${drop}&r&7) ${magicfind})&r

// registerWhen(register("chat", (trash, drop) => {
//     if (isDataLoaded() && isInSkyblock()) {
//         let magicFindMatch = drop.match(/\+(&r&b)?(\d+)%/);
//         let magicFind = magicFindMatch ? magicFindMatch[2] + '%' : null;
        
//         let amountMatch = drop.match(/&r&f&r&7(\d+)x/);
//         let amount = amountMatch ? amountMatch[1] : '1';
        
//         let dropNameMatch = drop.match(/&r&f(.+)&r&b/);
//         let dropName = dropNameMatch ? dropNameMatch[1].trim() : null;
        
//         dropArray = { magicFind, amount, dropName };
//     }
// }).setCriteria("${trash}&lRARE DROP! ${drop}"), () => settings.slayerDropDetect);
// //${trash}&lRARE DROP! &r&7(&r${amount} &r${drop}&r&7) ${magicfind})&r

// // "&r&9&lVERY RARE DROP! &r&7(&r&f&r&723x &r&f&r&9Nether Wart Distillate&r&7) &r&b(+391% &r&b✯ Magic Find&r&b)&r"

// //&7[15:33] &r&r&r&6&lRARE DROP! &r&fEnchanted Book &r&b(+&r&b281% &r&b✯ Magic Find&r&b)&r

        
// // for (let key in result) {
// //     print(key + ": " + result[key]);
// // }
