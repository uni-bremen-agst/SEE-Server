import { defineConfig } from "eslint/config";
import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import eslintReact from "@eslint-react/eslint-plugin";

export default defineConfig(

  {
    files: ["**/*.{js,mjs,cjs,ts,jsx,tsx}"],
    // rules: {
    //   semi: "error",
    // },
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      eslintReact.configs.recommended
    ]
  },

);
