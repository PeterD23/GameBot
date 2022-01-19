jest.mock('howlongtobeat', () => {
  return {
    HowLongToBeatService: jest.fn().mockImplementation(() => {
      return {
        search: jest.fn(),
        detail: () => {}
      };
    })
  };
});

jest.mock('./index', () => ({
  ...jest.requireActual('./index'),
  searchHltb: jest.fn(),
}));
jest.mock('howlongtobeat');
import { HowLongToBeatService } from 'howlongtobeat';

import { getGameHowLongToBeat, getGameRating, searchHltb } from "./index";

describe("Utilities", () => {
  describe("getHowLongGameToBeat", () => {
    beforeEach(() => {
      jest.clearAllMocks();
    });

    it("should call searchHltb with searchTerm", async() => {
  
      
    });
  });
});
