import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: "#4677ff",
      },
      fontFamily: {
        sans: ["Montserrat", "sans-serif"],
      },
    },
  },
  plugins: [],
} satisfies Config;
