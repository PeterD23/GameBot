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
const express_1 = __importDefault(require("express"));
const router = express_1.default.Router();
const utils_1 = require("../utils");
router.post("/", (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { game } = req.body;
        if (!game) {
            res.status(400).json({ error: "Please give a game in the request body" });
        }
        const gameHltb = yield (0, utils_1.getGameHowLongToBeat)(game);
        const gameRating = yield (0, utils_1.getGameRating)(game);
        res.status(200).json({ hltb: gameHltb, rating: gameRating });
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error });
    }
}));
module.exports = router;
