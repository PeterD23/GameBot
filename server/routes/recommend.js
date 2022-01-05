let hltb = require('howlongtobeat');
let hltbService = new hltb.HowLongToBeatService();

const express = require('express');
const router = express.Router();

router.post('/search', async(req, res) => {
	try {
		
		const {search} = req.body;
	
		if (!search) {
			return res.status(400).json({ error: 'No search given.' })
		}

		const result = await hltbService.search(search);

		res.json(result);

	} catch (error) {
		console.error(error);
		res.status(500).json({ error });
	}
});

router.post('/detail', async (req, res) => {
	try {
		const { id } = req.body;
		const result = await hltbService.detail(id);
		res.status(400).json({ result });
	} catch (error) {
		console.error(error);
		res.status(500).json({ error });
	}
});

module.exports = router;