/** Preflight is off because Mantine ships its own reset; Tailwind is used for layout and spacing utilities. */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  corePlugins: { preflight: false },
  theme: {
    extend: {
      colors: {
        ink: '#0F1B2D',
        paper: '#F3F5FA',
        signal: '#2F4BFF',
        sun: '#FFB547',
        mint: '#12B886',
      },
      fontFamily: {
        display: ['"Bricolage Grotesque"', 'system-ui', 'sans-serif'],
        body: ['Figtree', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
