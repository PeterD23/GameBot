jest.mock("howlongtobeat");
const hltb = require("howlongtobeat", () => {
  return jest.fn().mockImplementation(() => {
    return {
      search: () => {},
      detail: () => {},
    };
  });
});

import {
    searchHltb,
    getGameDetailsById,
    getGameHowLongToBeat,
    getGameRating,
  } from "./index";
  
const flushPromises = () => new Promise(setImmediate);


describe("Utilities", () => {
  describe("getHowLongGameToBeat", () => {
    beforeEach(() => {
      jest.clearAllMocks();
    });

    it("should call hltbService.search with searchTerm", async () => {
      const hltbService = new hltb.HowLongToBeatService();

      console.log(hltbService);
      hltbService.search.mockResolvedValue([{ name: "new game", id: 1 }]);

      const result = await searchHltb("Game Name");
      expect(hltbService.search).toHaveBeenCalledWith('hi');
    });
  });
});
