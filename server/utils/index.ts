const { HowLongToBeatService } = require('howlongtobeat');
const fetch = require('node-fetch');
import { openCriticClient } from './openCriticClient';
const hltbService = new HowLongToBeatService();


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
        const response: Array<any> = await openCriticClient.get(`http://api.opencritic.com/api/meta/search?criteria=${encodedName}`)

        const gameId: number = response[0].id;

        const ratingResponse = await openCriticClient.get(`http://api.opencritic.com/api/game/${gameId}`)

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
