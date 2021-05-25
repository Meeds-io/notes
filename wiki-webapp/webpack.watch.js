const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.prod.js');

// the display name of the war
const app = 'wiki';
//const exoServerPath = "/exo-server";
const exoServerPath = "/home/exo/Desktop/plf-enterprise-tomcat-standalone-6.2.x-notes-20210519.190553-1/platform-6.2.x-notes-SNAPSHOT";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`)
  },
  devtool: 'inline-source-map'
});
module.exports = config;

