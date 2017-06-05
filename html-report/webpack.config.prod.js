const UglifyJSPlugin = require('uglifyjs-webpack-plugin');
const webpack = require('webpack');
const path = require('path');


module.exports = function() {
  return {
    entry: './src/index.js',
    output: {
      path: path.join(__dirname, 'build'),
      filename: 'app.min.js'
    },
    resolve: {
      extensions: ['.js', '.json']
    },
    module: {
      rules: [
        { test: /\.js$/, loader: 'babel-loader', exclude: /node_modules/ }
      ]
    },
    plugins: [
      // config https://github.com/webpack-contrib/uglifyjs-webpack-plugin
      new UglifyJSPlugin()
    ]
  };
}
