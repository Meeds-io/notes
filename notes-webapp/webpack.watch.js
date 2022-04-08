const path = require('path');
const { merge } = require('webpack-merge');
const webpackCommonConfig = require('./webpack.prod.js');

// the display name of the war
const app = 'notes';

const exoServerPath = "/exo-server";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`)
  },
  mode: 'development',
  devtool: 'inline-source-map'
});
module.exports = config;

