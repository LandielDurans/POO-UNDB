import type { Config } from "tailwindcss";

// Cores como tokens semânticos (definidos em globals.css, em OKLCH).
// Trocar o tema = trocar as variáveis CSS, sem mexer nos componentes.
const color = (name: string) => `oklch(var(--${name}) / <alpha-value>)`;

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        canvas: color("canvas"),
        surface: color("surface"),
        sunk: color("sunk"),
        ink: color("ink"),
        muted: color("muted"),
        line: color("line"),
        brand: color("brand"),
        "brand-strong": color("brand-strong"),
        "brand-soft": color("brand-soft"),
        "on-brand": color("on-brand"),
        lane: color("lane"),
        success: color("success"),
        danger: color("danger"),
      },
      fontFamily: {
        display: ["var(--font-rubik)", "system-ui", "sans-serif"],
        sans: ["var(--font-rubik)", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
export default config;
