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


// Credits to DocilElm for this see here: https://github.com/DocilElm/Doc/blob/main/Doc/features/misc/CopyChat.js
const GuiChat = Java.type('net.minecraft.client.gui.GuiChat')
const Mouse = org.lwjgl.input.Mouse

registerWhen(register("guiMouseClick", (mouseX, mouseY, button, gui, event) => {
    if (!gui instanceof GuiChat) return;
    onMouseClick(button);
}), () => settings.copyChatMessage);

function onMouseClick(button) {
    if (button != 1 || !Client.isInChat()) return;
    const [ mouseX, mouseY ] = [Mouse.getX(), Mouse.getY()];
    const chatGui = Client.getChatGUI();
    const component = chatGui.func_146236_a(mouseX, mouseY)

    if (!component?.func_150261_e()) return;
    const guiScale = Client.settings.video.getGuiScale()
    const chatWidth = chatGui.func_146233_a(guiScale)

    let chatComponents = []

    for (let i = 0; i < chatWidth; i++) {
        let scannedComponent = chatGui.func_146236_a(i, mouseY)
        if (!scannedComponent) continue;

        chatComponents.push (
            Client.isControlDown()
                ? scannedComponent.func_150261_e()?.replace(/§/g, "&")
                : scannedComponent.func_150261_e().removeFormatting()
        )
        i += Renderer.getStringWidth(scannedComponent?.func_150265_g()) * guiScale
    }

    ChatLib.command(`ct copy ${chatComponents.join("")}`, true);
    ChatLib.chat("§6[SBO] §aCopied Chat Message!§r");
}
