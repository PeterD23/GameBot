import express, { Request, Response } from "express";
const router = express.Router();
import { hltbClient } from '../utils/HltbClient';

const getHltb = async (game: string) => {
  try {
    const gameHltb = await hltbClient.search(game);

    console.log(gameHltb);
    return gameHltb;
  } catch (error) {
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

    // const gameRating = await something();

    res.status(200).json({ hltb: gameHltb, rating: null });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error });
  }
});

export default router;
