const express = require('express');
const app = express();
const recommendRouter = require('./routes/recommend');

app.use('/recommend', recommendRouter);

app.use(express.json());
app.listen(2460, () => {
	console.log('Running HLTB listener on port 2460');
});
