const hltb = require('howlongtobeat');
const hltbService = new hltb.HowLongToBeatService();
const fetch = require('node-fetch');

const searchHltb = async(searchTerm) => hltbService.search(searchTerm);
const getGameDetailsById = async(id) => hltbService.detail(id);

const getGameHowLongToBeat = async(gameName) => {   
    const searchResults = await searchHltb(gameName);
    return getGameDetailsById(searchResults[0].id);
}

const getGameRating = async(game) => {
    const encodedName = encodeURIComponent(game);
    const url = `http://api.opencritic.com/api/meta/search?criteria=${encodedName}`;

    const response = await fetch(url);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(JSON.stringify(data));
    }

    const gameId = data[0].id;

    const ratingResponse = await fetch(`http://api.opencritic.com/api/game/${gameId}`)

    const ratingData = await ratingResponse.json();
    console.log(ratingData);
    return ratingData;
}

module.exports = { 
    searchHltb,
    getGameDetailsById,
    getGameHowLongToBeat,
    getGameRating
}

