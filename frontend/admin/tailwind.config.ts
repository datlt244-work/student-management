import type { Config } from 'tailwindcss'
import forms from '@tailwindcss/forms'
import containerQueries from '@tailwindcss/container-queries'

export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
    "../shared/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        "primary": "#f49d25",
        "primary-dark": "#e08510",
        "background-light": "#f8f7f5",
        "background-dark": "#221a10",
        "surface-light": "#ffffff",
        "surface-dark": "#1c160d",
        "text-main-light": "#1c160d",
        "text-main-dark": "#f8f7f5",
        "text-muted-light": "#9c7a49",
        "text-muted-dark": "#b09b78",
        "border-light": "#e8ddce",
        "border-dark": "#3e3223",
      },
      fontFamily: {
        "display": ["Lexend", "sans-serif"],
        "body": ["Noto Sans", "sans-serif"],
      },
    },
  },
  plugins: [
    forms,
    containerQueries,
  ],
} satisfies Config