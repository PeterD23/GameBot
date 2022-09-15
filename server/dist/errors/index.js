"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OPEN_CRITIC_ERROR = exports.HLTB_ERROR = void 0;
class HLTB_ERROR extends Error {
    constructor(message) {
        super(message);
        this.name = 'HLTB_ERROR';
    }
}
exports.HLTB_ERROR = HLTB_ERROR;
class OPEN_CRITIC_ERROR extends Error {
    constructor(message) {
        super(message);
        this.name = 'OPEN_CRITIC_ERROR';
    }
}
exports.OPEN_CRITIC_ERROR = OPEN_CRITIC_ERROR;
