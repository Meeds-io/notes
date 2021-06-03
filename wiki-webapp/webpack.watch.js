const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.prod.js');

// the display name of the war
const app = 'wiki';
const exoServerPath = "/home/exo/Desktop/feature notes/www/platform-6.2.x-SNAPSHOT";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`)
  },
  devtool: 'inline-source-map'
});
module.exports = config;

