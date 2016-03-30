var gulp = require('gulp'),
    less = require('gulp-less'),
    rename = require('gulp-rename'),
    cleanCSS = require('gulp-clean-css');

gulp.task('less', function () {
  gulp.src('public/css/override.less')
    .pipe(less())
    .pipe(cleanCSS())
    .pipe(rename('main.min.css'))
    .pipe(gulp.dest('public/css/'));
});

gulp.task('watch', function () {
    gulp.watch('public/css/*.less', ['less']);
});

gulp.task('default', ['less']);
