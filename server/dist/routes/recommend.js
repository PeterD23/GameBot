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
const { searchHltb, getDetailsHltb, getGameHowLongToBeat, getGameDetailsById, } = require("../utils");
const express_1 = __importDefault(require("express"));
const router = express_1.default.Router();
router.post("/search", (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { search } = req.body;
        if (!search) {
            return res.status(400).json({ error: "No search given." });
        }
        const result = yield searchHltb(search);
        res.status(200).json(result);
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error });
    }
}));
router.post("/detail", (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    try {
        const { id } = req.body;
        const result = yield getGameDetailsById(id);
        res.status(200).json({ result });
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error });
    }
}));
module.exports = router;
