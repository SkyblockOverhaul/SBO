import settings from "../settings";
import { BOLD, ITALIC } from "./constants";
import { registerWhen } from "./variables";
import { getWorld } from "./world";

const overlays = [];
const gui = new Gui();
export function openGUI() { gui.open() };

/**
 * Render scaled text on a graphical canvas or rendering context.
 *
 * @param {number} scale - The scale factor to apply to the text.
 * @param {string} text - The text to be rendered.
 * @param {number} x - The x-coordinate where the text will be rendered.
 * @param {number} y - The y-coordinate where the text will be rendered.
 */
function renderScale(scale, text, x, y) {
    Renderer.scale(scale);
    Renderer.drawString(text, x, y);
}

export class Overlay {
    /**
     * Creates an overlay with HUD elements and GUI functionality.
     *
     * @param {string} setting - The setting key used to determine whether the overlay should be shown.
     * @param {string[]} requires - An array of world names where the overlay should be displayed (or "all" for all requires).
     * @param {number[]} loc - An array representing the x, y, and scale of the overlay.
     * @param {string} command - The command name that will open the GUI.
     * @param {string} example - An example text to be displayed as an overlay.
     * @param {string} [optionalParam] - An optional parameter.
     */
    constructor(setting, requires, loc, command, example, optionalParam) {
        overlays.push(this);
        // Store the inputs as instance variables.
        this.setting = setting;
        this.requires = new Set(requires);
        this.loc = loc;
        this.X = this.loc[0] / this.loc[2];
        this.Y = this.loc[1] / this.loc[2];
        this.S = this.loc[2];
        this.example = example;
        this.message = example;
        this.gui = new Gui();
        this.optionalParam = optionalParam;
        this.setSize();

        // Register a command to open the GUI when executed.
        register("command", () => {
            this.gui.open();
        }).setName(command);

        // Register a render function to display the overlay and GUI instructions.
        // The overlay is shown when the GUI is open or in requires specified in 'requires' array.'
        registerWhen(register(this.requires.has("misc") ? "postGuiRender" : "renderOverlay", () => {
            if (this.gui.isOpen()) {
                // Coords and scale
                renderScale(
                    this.loc[2],
                    `${ITALIC}x: ${Math.round(this.loc[0])}, y: ${Math.round(this.loc[1])}, s: ${this.loc[2].toFixed(2)}`,
                    this.X, this.Y - 10
                );

                // Draw example text
                renderScale(this.loc[2], this.example, this.X, this.Y);

                // GUI Instructions
            } else if (settings[this.setting] && (this.requires.has(getWorld()) || this.requires.has("all")) && !gui.isOpen() && settings[this.optionalParam]) {
                if (this.requires.has("misc")) {
                    if (Player.getContainer().getName() !== "Paid Chest") return;
                    renderScale(this.loc[2], this.message, this.X, this.Y);
                } else  // Draw HUD
                    renderScale(this.loc[2], this.message, this.X, this.Y);
            }
        }), () => true);

        register("dragged", (dx, dy, x, y) => {
            if (this.gui.isOpen()) {
                // Changes location of text
                this.loc[0] = parseInt(x);
                this.loc[1] = parseInt(y);
                this.X = this.loc[0] / this.loc[2];
                this.Y = this.loc[1] / this.loc[2];
            }
        });
        
        register("guiKey", (char, keyCode, gui, event) => {
            if (this.gui.isOpen()) {
                if (keyCode === 13) {  // Increase Scale (+ key)
                    this.loc[2] += 0.05;
                    this.X = this.loc[0] / this.loc[2];
                    this.Y = this.loc[1] / this.loc[2];
                    this.S = this.loc[2];
                } else if (keyCode === 12) {  // Decrease Scale (- key)
                    this.loc[2] -= 0.05;
                    this.X = this.loc[0] / this.loc[2];
                    this.Y = this.loc[1] / this.loc[2];
                    this.S = this.loc[2];
                } else if (keyCode === 19) {  // Reset Scale (r key)
                    this.loc[2] = 1;
                    this.X = this.loc[0];
                    this.Y = this.loc[1];
                    this.S = this.loc[2];
                }
            }
        });
    }

    setSize() {
        this.width = 0;
        this.height = 0;
        this.example.split("\n").forEach(line => {
            this.width = Math.max(Renderer.getStringWidth(line) * this.loc[2], this.width);
            this.height += line.includes(BOLD) ? 10 * this.loc[2] : 5 * this.loc[2];
        });
    }
}
