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
    let mouseY = Mouse.getY();
    const chatGui = Client.getChatGUI();
    if (Client.isShiftDown()) {
        singleLineCopy(button, chatGui, mouseY);
        return;
    }
    const guiScale = Client.settings.video.getGuiScale();
    const chatWidth = chatGui.func_146233_a(guiScale);

    let line1 = [];
    let line2 = [];
    let line3 = [];
    let line4 = [];
    let line5 = [];

    let stopCopying = false;

    for (let i = 0; i < chatWidth; i ++) {
        if (chatGui.func_146236_a(i, mouseY)?.func_150261_e()) {
            if (!chatGui.func_146236_a(i, mouseY)?.func_150261_e()?.removeFormatting()?.startsWith(" ☠")) {
                while (chatGui.func_146236_a(i, mouseY)?.func_150261_e()?.removeFormatting()?.startsWith(" ")) {
                    mouseY = mouseY + (10 * guiScale)
                }
                break
            } else {
                break
            }
        }
    }

    for (let i = 0; i < chatWidth; i++) {
        let scannedComponent = chatGui.func_146236_a(i, mouseY);
        let scannedComponent2 = chatGui.func_146236_a(i, mouseY - (10 * guiScale));
        let scannedComponent3 = chatGui.func_146236_a(i, mouseY - (20 * guiScale));
        let scannedComponent4 = chatGui.func_146236_a(i, mouseY - (30 * guiScale));
        let scannedComponent5 = chatGui.func_146236_a(i, mouseY - (40 * guiScale));

        multilineCopy(scannedComponent, line1);
        if (i >= 0 && i <= 10) {
            if (scannedComponent2) {
                const text2 = scannedComponent2.func_150261_e()?.removeFormatting();
                if (text2.startsWith(" ") && !text2.startsWith(" ☠")) {
                    multilineCopy(scannedComponent2, line2, true);
                } else {
                    stopCopying = true;
                }
            }
            if (scannedComponent3 && !stopCopying) {
                const text3 = scannedComponent3.func_150261_e()?.removeFormatting();
                if (text3.startsWith(" ") && !text3.startsWith(" ☠")) {
                    multilineCopy(scannedComponent3, line3, true);
                } else {
                    stopCopying = true;
                }
            }
            if (scannedComponent4 && !stopCopying) {
                const text4 = scannedComponent4.func_150261_e()?.removeFormatting();
                if (text4.startsWith(" ") && !text4.startsWith(" ☠")) {
                    multilineCopy(scannedComponent4, line4, true);
                } else {
                    stopCopying = true;
                }
            }
            if (scannedComponent5 && !stopCopying) {
                const text5 = scannedComponent5.func_150261_e()?.removeFormatting();
                if (text5.startsWith(" ") && !text5.startsWith(" ☠")) {
                    multilineCopy(scannedComponent5, line5, true);
                } else {
                    stopCopying = true;
                }
            }
        }
        if (scannedComponent) i += (Renderer.getStringWidth(scannedComponent.func_150261_e()) * guiScale);
    }
    ChatLib.command(`ct copy ${[...line1, ...line2, ...line3, ...line4, ...line5].join("\n")}`, true);
    ChatLib.chat("§6[SBO] §aCopied Chat Message!§r");
}



function multilineCopy (comp, arr, secondLine = false) {
    if (!comp) return;
    const str = comp.func_150261_e()?.removeFormatting()
    if (secondLine && (!str.startsWith(" ") || /^ \(\d+\)$/.test(str))) return;
    const isControlDown = Client.isControlDown()

    const msg = isControlDown
        ? comp.func_150261_e()?.replace(/§/g, "&")
        : comp.func_150261_e()?.removeFormatting()?.replace(/§z/g, "")
    if (arr.indexOf(msg) !== -1) return

    arr.push(msg)
}

function singleLineCopy(button, chatGui, mouseY) {
    if (button != 1 || !Client.isInChat()) return;
    const mouseX = Mouse.getX();
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
                : scannedComponent.func_150261_e()?.removeFormatting()?.replace(/§z/g, "")
        )
        i += Renderer.getStringWidth(scannedComponent?.func_150265_g()) * guiScale
    }

    ChatLib.command(`ct copy ${chatComponents.join("")}`, true);
    ChatLib.chat("§6[SBO] §aCopied Chat Message!§r");
}