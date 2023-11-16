// TODO: Pull these from the client
const categories = [
  "Combat",
  "Player",
  "Movement",
  "Render",
  "World",
  "Misc",
  "Exploit",
  "Fun",
];

export type Category = (typeof categories)[number];

export default function useCategories() {
  return categories;
}
