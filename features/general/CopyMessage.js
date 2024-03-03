import settings from "../../settings";
import { registerWhen } from "../../utils/variables";

registerWhen(register("chat", (rest) => {
    let finalText = "RARE DROP! " + rest.removeFormatting();
    ChatLib.command(`ct copy ${finalText}`, true);
    ChatLib.chat("§6[SBO] §eCopied Rare Drop Message!§r");
}).setCriteria("&r&6&lRARE DROP! ${rest}"), () => settings.copyRareDrop);

