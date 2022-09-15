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
exports.hltbClient = void 0;
const errors_1 = require("../errors");
const fetch = require('node-fetch');
const BASE_URL = 'https://howlongtobeat.com/';
const SEARCH_URL = `${BASE_URL}api/search`;
const convertToHours = (seconds) => seconds / 3600;
const formatHLTBResponse = (games) => {
    try {
        const game = games[0];
        const formattedData = {
            name: game.game_name,
            pictureUrl: game.game_image,
            gameplayMain: convertToHours(game.comp_main),
            gameplayMainExtra: convertToHours(game.comp_plus),
            gameplayCompletionist: convertToHours(game.comp_all),
            releaseYear: game.release_world,
            developer: game.profile_dev,
            platforms: game.profile_platform,
            hltbScore: game.review_score
        };
        return formattedData;
    }
    catch (error) {
        throw new errors_1.HLTB_ERROR(`Error formatting results: ${error.toString()}`);
    }
};
exports.hltbClient = {
    search: (game) => __awaiter(void 0, void 0, void 0, function* () {
        console.log('game:::', game.split(' '));
        const response = yield fetch(SEARCH_URL, {
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
                searchOptions: {
                    games: {
                        platform: "",
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
            throw new errors_1.HLTB_ERROR(`Error with HLTB API:: ${error}`);
        }
        const hltbResponse = yield response.json();
        const { data } = hltbResponse;
        if (!data.length) {
            throw new errors_1.HLTB_ERROR('No Games Found');
        }
        return formatHLTBResponse(data);
    })
};
