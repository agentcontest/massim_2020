import resolve from '@rollup/plugin-node-resolve';
import typescript from '@rollup/plugin-typescript';
import commonjs from '@rollup/plugin-commonjs';

export default {
  input: 'js/main.ts',
  output: {
    file: 'src/main/resources/www/main.js',
    format: 'iife',
    name: 'Massim',
  },
  plugins: [
    resolve(),
    typescript(),
    commonjs({
      extensions: ['.js', '.ts'],
    }),
  ],
};
