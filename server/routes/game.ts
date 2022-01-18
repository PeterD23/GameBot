import express, { Request, Response } from "express";
const router = express.Router();
import { getGameHowLongToBeat, getGameRating } from "../utils";

router.post("/", async (req: Request, res: Response) => {
  try {
    const { game }: { game: string } = req.body;

    if (!game) {
      return res.status(400).json({ error: "Please give a game in the request body" });
    }

    const gameHltb = await getGameHowLongToBeat(game);

    if (!gameHltb) {
      return res.status(400).json({ error: "Could not find a HLTB for game" });
    }

    const gameRating = await getGameRating(game);

    if (!gameRating) {
      return res.status(400).json({ error: "Could not find game rating" });
    }
    
    res.status(200).json({ hltb: gameHltb, rating: gameRating });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error });
  }
});

export default router;
