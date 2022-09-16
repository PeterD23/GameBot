require('dotenv').config()
import express from "express";
const bodyParser = require("body-parser");
const app = express();
import gameRouter from "./routes/game"

app.use(bodyParser.json());

// Routes
app.use("/game", gameRouter);

app.listen(2460, () => {
  console.log("Running HLTB listener on port 2460");
});
