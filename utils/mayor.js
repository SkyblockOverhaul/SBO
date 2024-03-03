import { request } from "../../requestV2";
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

export function setNewMayorBool() {
    newMayor = true;
}

let dateMayorElected = undefined;
let newMayorAtDate = undefined;
let mayor = undefined;
let perks = new Set([]);
let mayorApiError = false;
function getYearMayorRequestV2() {
    mayor = undefined;
    perks = new Set([]);
    request({
        url: "https://api.hypixel.net/resources/skyblock/election",
        json: true
    }).then((response)=>{
        mayor = response.mayor.name;
        perks = new Set([...response.mayor.perks.map(perk => perk.name)]);
        mayorApiError = false;
    }).catch((error)=>{
        mayorApiError = true;
        console.error(error);
    });
}

function convertStringToDate(str) {
    var parts = str.split(".");
    var date = new Date(parts[2], parts[1] - 1, parts[0]);
    return date;
}

let skyblockDate = undefined;
let skyblockDateString = "";
function calcSkyblockDate() {
    let monthsInYear = 12;
    let secondsPerMinute = 0.8333333333333334;
    let secondsPerMonth = 37200;

    let unix = Math.floor(Date.now() / 1000);
    let secondsSinceLastLog = unix - 1560276000;
    let year = 1;
    let month = 1;
    let day = 1;
    let hour = 6;
    let minute = 0;

    let secondsPerYear = secondsPerMonth * monthsInYear;
    let secondsPerDay = 1200;
    let secondsPerHour = 50;

    let yearDiff = Math.floor(secondsSinceLastLog / secondsPerYear);
    secondsSinceLastLog -= yearDiff * secondsPerYear;
    year += yearDiff; // the only thing without bounds

    let monthDiff = Math.floor(secondsSinceLastLog / secondsPerMonth) % 13;
    secondsSinceLastLog -= monthDiff * secondsPerMonth;
    month = (month + monthDiff) % 13;

    let dayDiff = Math.floor(secondsSinceLastLog / secondsPerDay) % 32;
    secondsSinceLastLog -= dayDiff * secondsPerDay;
    day = (day + dayDiff) % 32;

    let hourDiff = Math.floor(secondsSinceLastLog / secondsPerHour) % 24;
    secondsSinceLastLog -= hourDiff * secondsPerHour;
    hour = (hour + hourDiff) % 24;

    if (hour < 6) { 
        if (day < 31) {
            day += 1;
        } else {
            day = 1;
            month += 1;
        }
    }

    let minuteDiff = Math.floor(secondsSinceLastLog / secondsPerMinute) % 60;
    secondsSinceLastLog -= minuteDiff * secondsPerMinute;
    minute = (minute + minuteDiff) % 60;

    minute = (Math.round(minute / 5) * 5) % 60;
    
    return day + "." + month + "." + year;
}

let refreshingMayor = false;
let newMayor = false;
register("step", () => {
    if (isInSkyblock()) {
        if (((mayor === undefined && mayorApiError) || newMayor) && !refreshingMayor) {
            refreshingMayor = true;
            getYearMayorRequestV2(); 
            refreshingMayor = false;
        }
    }
}).setFps(1);

register("worldLoad", () => {
    getYearMayorRequestV2(); 
});

register("step", () => { 
    if  (isInSkyblock()) {
        skyblockDateString = calcSkyblockDate();
        skyblockDate = convertStringToDate(skyblockDateString);
        if (dateMayorElected === undefined) {
            let compareDate = convertStringToDate("27.3." + skyblockDate.getFullYear());
            if (compareDate > skyblockDate) {
                dateMayorElected = convertStringToDate("27.3." + (skyblockDate.getFullYear() - 1));
                newMayorAtDate = convertStringToDate("27.3." + skyblockDate.getFullYear());
            }
            else {
                dateMayorElected = convertStringToDate("27.3." + skyblockDate.getFullYear());
                newMayorAtDate = convertStringToDate("27.3." + (skyblockDate.getFullYear() + 1));
            }
        }
        year = skyblockDate.getFullYear();
    }
}).setFps(1);

