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
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.getGameRating = exports.getGameHowLongToBeat = exports.getGameDetailsById = exports.searchHltb = void 0;
const hltb = require('howlongtobeat');
const hltbService = new hltb.HowLongToBeatService();
const node_fetch_1 = __importDefault(require("node-fetch"));
const searchHltb = (searchTerm) => __awaiter(void 0, void 0, void 0, function* () { return hltbService.search(searchTerm); });
exports.searchHltb = searchHltb;
const getGameDetailsById = (id) => __awaiter(void 0, void 0, void 0, function* () { return hltbService.detail(id); });
exports.getGameDetailsById = getGameDetailsById;
const getGameHowLongToBeat = (gameName) => __awaiter(void 0, void 0, void 0, function* () {
    const searchResults = yield searchHltb(gameName);
    return getGameDetailsById(searchResults[0].id);
});
exports.getGameHowLongToBeat = getGameHowLongToBeat;
const getGameRating = (game) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const encodedName = encodeURIComponent(game);
        const url = `http://api.opencritic.com/api/meta/search?criteria=${encodedName}`;
        const response = yield (0, node_fetch_1.default)(url);
        const data = yield response.json();
        if (!response.ok) {
            return;
        }
        const gameId = data[0].id;
        const ratingResponse = yield (0, node_fetch_1.default)(`http://api.opencritic.com/api/game/${gameId}`);
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
