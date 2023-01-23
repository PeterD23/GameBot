export type OpenCriticSearchResult = Array<OpenCriticSearchGameObject>

export interface OpenCriticSearchGameObject {
    id: number;
    name: string;
    dist: number;
}

export interface OpenCriticGameObject {
    // Has other fields but we don't care about those.
    hasLootBoxes?: boolean;
    medianScore: number;
    name: string;
    description: string;
    genre?: Array<any>
}
