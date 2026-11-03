import { createTheme } from '@mantine/core';

const signal = ['#eef1ff', '#dde3ff', '#bac5ff', '#93a4ff', '#7087ff', '#556fff', '#2f4bff', '#2339db', '#1b2db5', '#142191'];

export const theme = createTheme({
  primaryColor: 'signal',
  primaryShade: 6,
  colors: { signal },
  fontFamily: 'Figtree, system-ui, sans-serif',
  headings: { fontFamily: '"Bricolage Grotesque", system-ui, sans-serif', fontWeight: '700' },
  defaultRadius: 'md',
  black: '#0f1b2d',
  components: {
    Button: { defaultProps: { fw: 600 } },
    TextInput: { defaultProps: { size: 'md' } },
    PasswordInput: { defaultProps: { size: 'md' } },
    Select: { defaultProps: { size: 'md', allowDeselect: false } },
    Textarea: { defaultProps: { size: 'md' } },
    NumberInput: { defaultProps: { size: 'md' } },
    TagsInput: { defaultProps: { size: 'md' } },
  },
});
