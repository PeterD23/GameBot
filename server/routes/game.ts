import express, { Request, Response } from "express";
const router = express.Router();
import { OpenCriticSearchGameObject } from '../interfaces/OpenCritic';
import { hltbClient } from '../utils/HltbClient';
import { openCriticClient } from "../utils/openCriticClient";


const ACCURACY_THRESHOLD = 0.4;
const getHltb = async (game: string) => {
  try {
    const gameHltb = await hltbClient.search(game);
    return gameHltb;
  } catch (error) {
    console.error(error);
    return null;
  }
}

const getRating = async(game: string) => {
  try { 
    const foundGame: OpenCriticSearchGameObject = await openCriticClient.search(game);

    if(foundGame.dist > ACCURACY_THRESHOLD) { 
      return null;
    }

    const rating = await openCriticClient.getGame(foundGame.id);
    return rating;
  } catch(error) { 
    console.error(error);
    return null;
  }
}
router.post("/", async (req: Request, res: Response) => {
  try {
    const { game }: { game: string } = req.body;

    if (!game) {
      return res.status(400).json({ error: "Please give a game in the request body" });
    }

    const gameHltb = await getHltb(game);

    const rating = await getRating(game);


    res.status(200).json({ hltb: gameHltb, rating});

  } catch (error) {
    console.error(error);
    res.status(500).json({ error });
  }
});

export default router;
