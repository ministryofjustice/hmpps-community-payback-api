function addDays(date, days) {
    var result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
}

function toIsoDate(date) {
    return date.toISOString().slice(0, 10)
}

export {addDays, toIsoDate};