const path = require('path')
const { VueLoaderPlugin } = require('vue-loader')
var webpack = require('webpack');
require('babel-polyfill')

module.exports = {
  mode: 'development',
  entry: [
    'babel-polyfill',
    './src/app'
  ],
  output: {
    path: path.resolve(__dirname, 'dist'),
    publicPath: '/dist/',
    filename: 'build.js'
  },
  module: {
    rules: [
      {
        test: /\.json$/,
        type: 'javascript/auto',
        use: "json-loader" 
      },
      {
        test: /\.js/,
        //exclude: /(node_modules)/,
        use: 'babel-loader'
      },
      {
        test: /\.vue$/,
        use: 'vue-loader'
      },
      {
        test: /\.css$/,
        use: [
          'vue-style-loader',
          'css-loader'
        ]
      }
    ]
  },
  plugins: [
    new VueLoaderPlugin(),
    new webpack.DefinePlugin({
      VERSION: JSON.stringify(require("./package.json").version)
    })
  ]
}