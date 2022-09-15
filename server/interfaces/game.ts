export interface HLTBGameFormattedResult {
    name: string;
    pictureUrl?: string;
    developer?: string;
    gameplayMain: number;
    gameplayMainExtra: number;
    gameplayCompletionist: number;
    hltbScore?: number;
    releaseYear?: number;
    platforms?: string;
}

export interface OpenCriticFormattedResult {

}

export interface GamResponse {
    hltb: HLTBGameFormattedResult | null;
    rating: OpenCriticFormattedResult | null;
}
