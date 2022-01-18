import express, { Request, Response } from "express";
const router = express.Router();
const { getGameRating } = require("../utils");

router.post("/", async (req: Request, res: Response) => {
  try {
    const { game } = req.body;

    const ratingData = await getGameRating(game);

    res.status(200).json({ ratingData });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error });
  }
});

module.exports = router;
