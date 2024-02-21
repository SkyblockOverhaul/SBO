import { request } from "../../requestV2";
import { registerWhen } from "./variables";
import { isInSkyblock } from "./functions";
/**
 * Gets the array of mayor's perks.
 *
 * @returns {string[]} - An array containing the names of the mayor's perks.
 */
export function getPerks() {
    return perks;
}

export function getMayor() {
    return mayor;
}

export function getYear() {
    return year;
}

/**
 * @returns {date} - skyblock date
 */
export function getSkyblockDate() {
    return skyblockDate;
}

export function getSkyblockDateString() {
    return skyblockDateString;
}

export function getNewMayorAtDate() {
    return newMayorAtDate;
}

export function setDateMayorElected(dateString) {
    dateMayorElected = convertStringToDate(dateString);
}

export function getDateMayorElected() {
    return dateMayorElected;
}



function getYearMayorRequestV2() {
    request({
        url: "https://api.hypixel.net/resources/skyblock/election",
        json: true
    }).then((response)=>{
        mayor = response.mayor.name;
        dateMayorElected = convertStringToDate("27.3." + (response.mayor.election.year + 1));
        newMayorAtDate = convertStringToDate("27.3." + (response.mayor.election.year + 2));
        year = response.current.year;
        perks = new Set([...response.mayor.perks.map(perk => perk.name)]);
    }).catch((error)=>{
        console.error(error);
    });

    return year, mayor;

}

function convertStringToDate(str) {
    var parts = str.split(".");
    var date = new Date(parts[2], parts[1] - 1, parts[0]);
    return date;
}


function convertDate(dateStr) {
    if (dateStr === undefined || dateStr == "") return "";
    var seasonToMonth = {
        'Early Spring': '1',
        "Spring": '2',
        "Late Spring": '3',
        "Early Summer": '4',
        "Summer": '5',
        "Late Summer": '6',
        "Early Autumn": '7',
        "Autumn": '8',
        "Late Autumn": '9',
        "Early Winter": '10',
        "Winter": '11',
        "Late Winter": '12'
    };

    dateStr = dateStr.toString().replace(/[^a-z0-9\s-]/gi, '');
    var parts = dateStr.split(' ');
    if (parts.length === 4) {
        season = parts[1] + ' ' + parts[2];
        day = parts[3];
    }
    else {
        season = parts[1];
        day = parts[2];
    }
    
    day = day.replace(/(st|nd|rd|th)$/, '');

    var month = seasonToMonth[season] || '';
    
    if (lastMonth == 12 && month == 1) {  // if the last month was December, and the current month is January, then the year has changed
        year++;
    }
    else {
        year = getYear();
    }
    skyblockDateString = day + '.' + month + '.' + year;
    skyblockDate = convertStringToDate(skyblockDateString);
    lastMonth = month;
    return skyblockDateString, skyblockDate;
}



let mayor = undefined;
let year = 0;
let skyblockDate = undefined;
let skyblockDateString = "";
let perks = new Set([]);
let lastMonth = 0;
let dateMayorElected = undefined;
let newMayorAtDate = undefined;

register("worldLoad", () => {
    year, mayor = getYearMayorRequestV2();
});

registerWhen(register("step", () => { 
    scoreboardLines = Scoreboard.getLines();
    skyblockDateString, skyblockDate = convertDate(scoreboardLines[scoreboardLines.length - 3]);
    ChatLib.chat(skyblockDateString);
}).setFps(1), () => isInSkyblock());
