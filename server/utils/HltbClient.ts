import { HLTB_ERROR } from '../errors';
import { HLTBGameFormattedResult } from '../interfaces/game';
import { HowLongToBeatSearchResult, HowLongToBeatGame } from '../interfaces/howLongToBeat'
const fetch = require('node-fetch');

const BASE_URL = 'https://howlongtobeat.com/'
const SEARCH_URL = `${BASE_URL}api/search`;

const convertToHours = (seconds: number) => seconds / 3600;

const formatHLTBResponse = (games: Array<HowLongToBeatGame>) => {
    try {
        const game = games[0];
        const formattedData: HLTBGameFormattedResult = {
            name: game.game_name,
            imageUrl: game.game_image,
            gameplayMain: convertToHours(game.comp_main).toFixed(1),
            gameplayMainExtra: convertToHours(game.comp_plus).toFixed(1),
            gameplayCompletionist: convertToHours(game.comp_all).toFixed(1),
            releaseYear: game.release_world,
            developer: game.profile_dev,
            platforms: game.profile_platform,
            hltbScore: game.review_score
        }
        return formattedData;
    } catch (error: Error | any) {
        throw new HLTB_ERROR(`Error formatting results: ${error.toString()}`);
    }
}

export const hltbClient = {
    search: async (game: string) => {
        const response = await fetch(SEARCH_URL, {
            method: 'POST',
            headers: {
                'origin': BASE_URL,
                'referer': BASE_URL,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                searchType: "games",
                searchTerms: game.split(' '),
                searchPage: 1,
                size: 3,
                searchOptions:
                {
                    games: {
                        platform: "", // DO NOT REMOVE
                        sortCategory: "popular",
                        rangeCategory: "main",
                        rangeTime: {
                            min: 0,
                            max: 0
                        },
                    },
                    users: {
                        sortCategory: "postcount"
                    },
                }
            })
        });

        if (!response.ok) {
            const error = (response && response.message) || response.status;
            throw new HLTB_ERROR(`Error with HLTB API:: ${error}`)
        }

        const hltbResponse: HowLongToBeatSearchResult = await response.json();

        const { data } = hltbResponse;

        if (!data.length) {
            throw new HLTB_ERROR('No Games Found');
        }

        return formatHLTBResponse(data);
    }
}
