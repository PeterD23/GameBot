"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const bodyParser = require("body-parser");
const app = (0, express_1.default)();
const recommend_1 = __importDefault(require("./routes/recommend"));
const rating_1 = __importDefault(require("./routes/rating"));
const game_1 = __importDefault(require("./routes/game"));
app.use(bodyParser.json());
// Routes
app.use("/recommend", recommend_1.default);
app.use("/rating", rating_1.default);
app.use("/game", game_1.default);
app.listen(2460, () => {
    console.log("Running HLTB listener on port 2460");
});
