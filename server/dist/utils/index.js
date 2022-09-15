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
exports.getGameRating = exports.getGameHowLongToBeat = exports.getGameDetailsById = exports.searchHltb = void 0;
const { HowLongToBeatService } = require('howlongtobeat');
const fetch = require('node-fetch');
const openCriticClient_1 = require("./openCriticClient");
const hltbService = new HowLongToBeatService();
const searchHltb = (searchTerm) => __awaiter(void 0, void 0, void 0, function* () { return hltbService.search(searchTerm); });
exports.searchHltb = searchHltb;
const getGameDetailsById = (id) => __awaiter(void 0, void 0, void 0, function* () { return hltbService.detail(id); });
exports.getGameDetailsById = getGameDetailsById;
const getGameHowLongToBeat = (gameName) => __awaiter(void 0, void 0, void 0, function* () {
    const searchResults = yield searchHltb(gameName);
    if (!(searchResults === null || searchResults === void 0 ? void 0 : searchResults.length)) {
        return '';
    }
    return getGameDetailsById(searchResults[0].id);
});
exports.getGameHowLongToBeat = getGameHowLongToBeat;
const getGameRating = (game) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const encodedName = encodeURIComponent(game);
        const response = yield openCriticClient_1.openCriticClient.get(`http://api.opencritic.com/api/meta/search?criteria=${encodedName}`);
        const gameId = response[0].id;
        const ratingResponse = yield openCriticClient_1.openCriticClient.get(`http://api.opencritic.com/api/game/${gameId}`);
        if (!ratingResponse.ok) {
            return;
        }
        const ratingData = yield ratingResponse.json();
        return ratingData;
    }
    catch (error) {
        console.error(error);
    }
});
exports.getGameRating = getGameRating;
