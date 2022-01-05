const express = require('express');
const router = express.Router();
const fetch = require('node-fetch');

router.post('/', async (req, res) => {
    try {
        const { game } = req.body;
        const encodedName = encodeURIComponent(game);

        const url = `http://api.opencritic.com/api/meta/search?criteria=${encodedName}`;

        const response = await fetch(url);


        const data = await response.json();

        if (!response.ok) {
            
            throw new Error(JSON.stringify(data));
        }

        const gameId = data[0].id;

        const ratingResponse = await fetch(`http://api.opencritic.com/api/review/game/${gameId}`)

        const ratingData = await ratingResponse.json();

        res.status(200).json({ ratingData });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error });
    }
});


module.exports = router;