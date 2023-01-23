# GameBot Server

## Introduction

This server is for the Gamebot to use locally in order to access services such as How Long To Beat and OpenCritic API.

## Prerequisites

Node 14

## Setup

Add a .env file under /server
Add the following:

OPEN_CRITIC_API_KEY={KEY_HERE}
RAPID_API_HOST=opencritic-api.p.rapidapi.com

Then do the following commands:

- npm i
- npm run build
- npm run start