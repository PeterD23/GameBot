import express from "express";
const bodyParser = require("body-parser");
const app = express();
import recommendRouter from "./routes/recommend"
import ratingRouter from "./routes/rating"
import gameRouter from "./routes/game"

app.use(bodyParser.json());

// Routes
app.use("/recommend", recommendRouter);
app.use("/rating", ratingRouter);
app.use("/game", gameRouter);

app.listen(2460, () => {
  console.log("Running HLTB listener on port 2460");
});
