import settings from "../../settings";
import { registerWhen } from "../../utils/variables";

let finalText = "";
function copyRareDrop(drop, type) {
    if (type != "") {
        finalText = type + " RARE DROP! " + drop.removeFormatting();
    }
    else {
        finalText = "RARE DROP! " + drop.removeFormatting();
    }
    ChatLib.command(`ct copy ${finalText}`, true);
    ChatLib.chat("§6[SBO] §eCopied Rare Drop Message!§r");
}

registerWhen(register("chat", (trash, type, drop) => {
    copyRareDrop(drop, type);
}).setCriteria("${trash}&l${type} RARE DROP! ${drop}"), () => settings.copyRareDrop);

registerWhen(register("chat", (trash, drop) => {
    copyRareDrop(drop, "");
}).setCriteria("${trash}&lRARE DROP! ${drop}"), () => settings.copyRareDrop);
// RARE DROP! (Foul Flesh) (+309% ✯ Magic Find)
// &r&b&lRARE DROP! &r&7(&r&f&r&9Foul Flesh&r&7) &r&b(+309% &r&b✯ Magic Find&r&b)&r

// CRAZY RARE DROP! (Revenant Catalyst) (+364% ✯ Magic Find)
// &r&d&lCRAZY RARE DROP! &r&7(&r&f&r&5Revenant Catalyst&r&7) &r&b(+364% &r&b✯ Magic Find&r&b)&r

// VERY RARE DROP! (◆ Pestilence Rune I) (+364% ✯ Magic Find)
// &r&5&lVERY RARE DROP! &r&7(&r&f&r&2◆ Pestilence Rune I&r&7) &r&b(+364% &r&b✯ Magic Find&r&b)&r

// const GuiChat = Java.type('net.minecraft.client.gui.GuiChat')
// register("postGuiRender", (gui,x,y) => {
//     if (!gui instanceof GuiChat) return;
//     if (Client.isInChat()){
//         print(Client.getChatGUI()?.func_146236_a(Client.getMouseX(), Client.getMouseY()));
//     }
// });