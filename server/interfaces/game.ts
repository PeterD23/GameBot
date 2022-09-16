export interface HLTBGameFormattedResult {
    name: string;
    imageUrl?: string;
    developer?: string;
    gameplayMain: string;
    gameplayMainExtra: string;
    gameplayCompletionist: string;
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
