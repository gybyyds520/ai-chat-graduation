export default {
  plugins: {
    autoprefixer: {
      overrideBrowserslist: [
        'last 2 versions',
        '> 1%',
        'iOS >= 10',
        'Android >= 5',
        'Chrome >= 49',
        'Firefox >= 45',
        'Safari >= 10',
        'Edge >= 12'
      ]
    }
  }
}