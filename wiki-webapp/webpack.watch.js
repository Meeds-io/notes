const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.prod.js');

// the display name of the war
const app = 'wiki';

const exoServerPath = "/exo-server";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`)
  },
  devtool: 'inline-source-map'
});
module.exports = config;

