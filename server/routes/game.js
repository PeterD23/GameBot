const express = require('express');
const router = express.Router();
const { getGameHowLongToBeat, getGameRating } = require('../utils');

router.post('/', async(req, res) => {
    try { 
        const { game } = req.body;

        if(!game){ 
            res.status(400).json({ error: 'Please give a game in the request body'});
        }

        const gameHltb = await getGameHowLongToBeat(game);

        const gameRating = await getGameRating(game);
        
        res.status(200).json({ hltb: gameHltb, rating: gameRating });
    } catch(error) { 
        console.error(error);
        res.status(500).json({ error });
    }
});

module.exports = router;