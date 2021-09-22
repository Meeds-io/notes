const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.prod.js');

// the display name of the war
const app = 'notes';

const exoServerPath = "/home/exo/Downloads/plf-enterprise-tomcat-standalone-6.3.x-ux-20210915.032051-317/platform-6.3.x-ux-SNAPSHOT";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`)
  },
  devtool: 'inline-source-map'
});
module.exports = config;

