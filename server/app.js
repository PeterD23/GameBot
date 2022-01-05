const express = require('express');
const bodyParser = require('body-parser');
const app = express();
const recommendRouter = require('./routes/recommend');
const ratingRouter = require('./routes/rating');


app.use(bodyParser.json());

// Routes
app.use('/recommend', recommendRouter);
app.use('/rating', ratingRouter);

app.listen(2460, () => {
	console.log('Running HLTB listener on port 2460');
});
