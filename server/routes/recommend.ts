const {
  searchHltb,
  getDetailsHltb,
  getGameHowLongToBeat,
  getGameDetailsById,
} = require("../utils");
import express, { Request, Response } from "express";
const router = express.Router();

router.post("/search", async (req: Request, res: Response) => {
  try {
    const { search }: { search: string} = req.body;

    if (!search) {
      return res.status(400).json({ error: "No search given." });
    }

    const result = await searchHltb(search);

    res.status(200).json(result);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error });
  }
});

router.post("/detail", async (req: Request, res: Response) => {
  try {
    const { id }: { id: number } = req.body;
    const result = await getGameDetailsById(id);
    res.status(200).json({ result });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error });
  }
});

module.exports = router;