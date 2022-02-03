const { HowLongToBeatService } = require('howlongtobeat');
const hltbService = new HowLongToBeatService();
const fetch = require('node-fetch');

const searchHltb = async(searchTerm: string) => hltbService.search(searchTerm);
const getGameDetailsById = async(id: number) => hltbService.detail(id);

const getGameHowLongToBeat = async(gameName: string) => {   
    const searchResults = await searchHltb(gameName);

    if(!searchResults?.length){ 
        return '';
    }
    return getGameDetailsById(searchResults[0].id);
}

const getGameRating = async(game: string) => {
    try { 

    const encodedName = encodeURIComponent(game);
    const url = `http://api.opencritic.com/api/meta/search?criteria=${encodedName}`;

    const response = await fetch(url);
    const searchResults:any = await response.json();

    if (!response.ok || !searchResults?.length) {
        return;
    }

    const gameId:number = searchResults[0].id;

    const ratingResponse = await fetch(`http://api.opencritic.com/api/game/${gameId}`)

    if (!ratingResponse.ok) {
        return;
    }

    const ratingData = await ratingResponse.json();
    return ratingData;
    } catch(error){ 
        console.error(error);
    }
}

export { 
    searchHltb,
    getGameDetailsById,
    getGameHowLongToBeat,
    getGameRating
}