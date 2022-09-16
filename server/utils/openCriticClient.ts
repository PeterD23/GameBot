const fetch = require('node-fetch');
import  { OpenCriticSearchResult, OpenCriticGameObject } from '../interfaces/OpenCritic';

import { OPEN_CRITIC_ERROR } from '../errors';

const openCriticHeaders = {
    'X-RapidAPI-Key': process.env.OPEN_CRITIC_API_KEY,
    'X-RapidAPI-Host': process.env.RAPID_API_HOST
}

const BASE_URL = 'https://opencritic-api.p.rapidapi.com';
const SEARCH_URL = `${BASE_URL}/game/search`;
const GAME_DETAILS_URL = `${BASE_URL}/game`

export const openCriticClient = {
    search: async (game: string) => {
        const url = `${SEARCH_URL}?criteria=${encodeURIComponent(game)}`
        const response = await fetch(url, {
            method: 'GET',
            headers: openCriticHeaders,
        });

        if(!response.ok){ 
            const error = (response && response.message) || response.status;
            console.error(error);
            throw new OPEN_CRITIC_ERROR('API Error');
        }

        const data:OpenCriticSearchResult = await response.json();

        if(!data.length) { 
            throw new OPEN_CRITIC_ERROR('No Games Found');
        }

        return data[0];
    },
    getGame: async (id: number) => {
        const response = await fetch(`${GAME_DETAILS_URL}/${id}`, {
            method: 'GET',
            headers: openCriticHeaders
        });

        if (!response.ok) {
            const error = (response && response.message) || response.status;
            console.error(error);
            throw new OPEN_CRITIC_ERROR('Game Details Api error')
        }

        const game:OpenCriticGameObject = await response.json();

        console.log(game);

        if (!game) {
            throw new OPEN_CRITIC_ERROR('No Game Found');
        }

        return { 
            hasLootBoxes: game.hasLootBoxes,
            medianScore: game.medianScore,
            name: game.name,
            description: game.description,
            genre: game.genre
        }
}}
