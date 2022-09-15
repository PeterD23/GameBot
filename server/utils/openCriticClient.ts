const fetch = require('node-fetch');
import { OPEN_CRITIC_ERROR } from '../errors';

const openCriticHeaders = {
    'X-RapidAPI-Key': process.env.OPEN_CRITIC_API_KEY,
    'X-RapidAPI-Host': process.env.RAPID_API_HOST
}

const BASE_URL = 'https://opencritic-api.p.rapidapi.com';
const SEARCH_URL = `${BASE_URL}/game/search`;
const GAME_DETAILS_URL = `${BASE_URL}/id`

const formatOpenCriticResponse = (data: any) => {


}



export const openCriticClient = {
    search: async (game: string) => {
        const url = `${SEARCH_URL}?criteria=${encodeURIComponent(game)}`
        const response = await fetch(url, {
            method: 'POST',
            headers: openCriticHeaders,

        });
        return response.json();
    },
    get: async (url: string) => {
        const response = await fetch(url, {
            method: 'GET',
            headers: openCriticHeaders
        });
        if (!response.ok) {
            throw new OPEN_CRITIC_ERROR('Error getting games from OpenCritic')
        }
        const data = await response.json();

        if (!data || !data.length) {
            throw new OPEN_CRITIC_ERROR('No Game Found');
        }

        return data;
    }
}
