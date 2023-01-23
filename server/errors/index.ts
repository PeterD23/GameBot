export class HLTB_ERROR extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'HLTB_ERROR';
    }
}

export class OPEN_CRITIC_ERROR extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'OPEN_CRITIC_ERROR';
    }
}
