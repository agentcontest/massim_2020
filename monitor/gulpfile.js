const gulp = require('gulp');
const browserify = require('browserify');
const tsify = require('tsify');
const watchify = require('watchify');
const buffer = require('vinyl-buffer');
const source = require('vinyl-source-stream');

const www = 'src/main/resources/www/';

function build() {
  return browserify('js/main.ts', {
      standalone: 'Monitor',
      debug: false
    })
    .plugin(tsify);
}

const watchedBrowserify = watchify(build());

function bundle() {
  return watchedBrowserify
    .bundle()
    .on('error', (e) => console.error(e.message))
    .pipe(source('main.js'))
    .pipe(buffer())
    .pipe(gulp.dest(www));
}

watchedBrowserify.on('update', bundle);
watchedBrowserify.on('log', console.log);

function dev() {
  return build()
    .bundle()
    .pipe(source('main.js'))
    .pipe(gulp.dest(www));
}

exports.dev = dev;
exports.watch = bundle;
exports.default = dev;
