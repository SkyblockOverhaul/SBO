import { drawRectangleOutline as outline } from "../../utils/functions";

const PartyFinderGUI = new Gui()
let currentPage = 0
PartyFinderGUI.registerDraw(partyFinderRender);
PartyFinderGUI.registerClosed(partyFinderClose);

function getLayoutData() {
    let displayX = Renderer.screen.getWidth()
    let displayY = Renderer.screen.getHeight()

    let pfWindowWidth = displayX * 0.65
    let pfWindowHeight = displayY * 0.7
    let pfWindowX = (displayX - pfWindowWidth) / 2
    let pfWindowY = (displayY - pfWindowHeight) / 2

    let boxWidth = 200
    let boxHeight = 35
    let spacingY = 10
    let spacingX = 0
    
    let columns = Math.floor((displayX * 0.75 - spacingX) / (boxWidth + spacingX));
    let rows = Math.floor((displayY * 0.6 - spacingY) / (boxHeight + spacingY));

    let totalWidth = columns * boxWidth + (columns - 1) * spacingX
    let totalHeight = rows * boxHeight + (rows - 1) * spacingY

    let startX = (displayX - totalWidth) / 2
    let startY = (displayY - totalHeight) / 2


    return {
        displayX,
        displayY,
        boxWidth,
        boxHeight,
        spacingX,
        spacingY,
        columns,
        rows,
        totalWidth,
        totalHeight,
        startX,
        startY,
        pfWindowWidth,
        pfWindowHeight,
        pfWindowX,
        pfWindowY
    }
}

function partyFinderRender() {
    const layoutData = getLayoutData()

    roundRect(color(30, 30, 30, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 10);
}

function partyFinderClose() {
    currentPage = 0
}

register("command", () => {
    currentPage = 0
    PartyFinderGUI.open()
}).setName("sbopartyfinder").setAliases("sbopf")

function rect(color, x, y, width, height) {
    Renderer.drawRect(color, x, y, width, height);
}

function color(r, g, b, a) {
    return Renderer.color(r, g, b, a)
}

function roundRect(color, x, y, width, height, radius) {
    radius = Math.min(radius, width / 2, height / 2);

    rect(color, x + radius, y + radius, width - 2 * radius, height - 2 * radius);

    rect(color, x + radius, y, width - 2 * radius, radius); // Top rectangle
    rect(color, x + radius, y + height - radius, width - 2 * radius, radius); // Bottom rectangle
    rect(color, x, y + radius, radius, height - 2 * radius); // Left rectangle
    rect(color, x + width - radius, y + radius, radius, height - 2 * radius); // Right rectangle

    Renderer.drawCircle(color, x + radius, y + radius, radius, 100, 5); // Top-left corner
    Renderer.drawCircle(color, x + width - radius, y + radius, radius, 100, 5); // Top-right corner
    Renderer.drawCircle(color, x + width - radius, y + height - radius, radius, 100, 5); // Bottom-right corner
    Renderer.drawCircle(color, x + radius, y + height - radius, radius, 100, 5); // Bottom-left corner
}