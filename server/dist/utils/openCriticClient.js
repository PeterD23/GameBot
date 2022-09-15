"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.openCriticClient = void 0;
const fetch = require('node-fetch');
const errors_1 = require("../errors");
const openCriticHeaders = {
    'X-RapidAPI-Key': process.env.OPEN_CRITIC_API_KEY,
    'X-RapidAPI-Host': process.env.RAPID_API_HOST
};
const BASE_URL = 'https://opencritic-api.p.rapidapi.com';
const SEARCH_URL = `${BASE_URL}/game/search`;
const GAME_DETAILS_URL = `${BASE_URL}/id`;
const formatOpenCriticResponse = (data) => {
};
exports.openCriticClient = {
    search: (game) => __awaiter(void 0, void 0, void 0, function* () {
        const url = `${SEARCH_URL}?criteria=${encodeURIComponent(game)}`;
        const response = yield fetch(url, {
            method: 'POST',
            headers: openCriticHeaders,
        });
        return response.json();
    }),
    get: (url) => __awaiter(void 0, void 0, void 0, function* () {
        const response = yield fetch(url, {
            method: 'GET',
            headers: openCriticHeaders
        });
        if (!response.ok) {
            throw new errors_1.OPEN_CRITIC_ERROR('Error getting games from OpenCritic');
        }
        const data = yield response.json();
        if (!data || !data.length) {
            throw new errors_1.OPEN_CRITIC_ERROR('No Game Found');
        }
        return data;
    })
};
