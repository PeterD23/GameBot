
const { searchHltb, getDetailsHltb, getGameHowLongToBeat } = require('../utils');
const express = require('express');
const router = express.Router();

router.post('/search', async(req, res) => {
	try {
		
		const {search} = req.body;
	
		if (!search) {
			return res.status(400).json({ error: 'No search given.' })
		}

		const result = await searchHltb(search);

		res.status(200).json(result);

	} catch (error) {
		console.error(error);
		res.status(500).json({ error });
	}
});

router.post('/detail', async (req, res) => {
	try {
		const { id } = req.body;
		const result = await getGameDetailsById(id);
		res.status(200).json({ result });
	} catch (error) {
		console.error(error);
		res.status(500).json({ error });
	}
});

module.exports = router;