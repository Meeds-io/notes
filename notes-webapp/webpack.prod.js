const path = require('path');
const { styles } = require( '@ckeditor/ckeditor5-dev-utils' );
const TerserPlugin = require('terser-webpack-plugin');
const ESLintPlugin = require('eslint-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader')

const config = {
  mode: 'production',
  context: path.resolve(__dirname, '.'),
  entry: {
    wikiCkeditor: './src/main/webapp/javascript/eXo/wiki/ckeditor/wikiCkeditor.js',
    pageContent: './src/main/webapp/javascript/eXo/wiki/pageContent.js',
    wikiSearchCard: './src/main/webapp/vue-app/wikiSearch/main.js',
    notes: './src/main/webapp/vue-app/notes/main.js',
    notesEditor: './src/main/webapp/vue-app/notes-editor/main.js',
    notesSwitch: './src/main/webapp/vue-app/notes-switch/main.js'
  },
  output: {
    publicPath: '',
    path: path.join(__dirname, 'target/notes/'),
    filename: 'javascript/[name].bundle.js',
    libraryTarget: 'amd'
  },
  plugins: [
    new ESLintPlugin({
      files: [
        './src/main/webapp/vue-app/*.js',
        './src/main/webapp/vue-app/*.vue',
        './src/main/webapp/vue-app/**/*.js',
        './src/main/webapp/vue-app/**/*.vue',
      ],
    }),
    new VueLoaderPlugin()
  ],
  module: {
    rules: [
     {
       // Or /ckeditor5-[^/]+\/theme\/icons\/[^/]+\.svg$/ if you want to limit this loader
       // to CKEditor 5 icons only.
       test: /\.svg$/,
       use: [ 'raw-loader' ]
      },
      {
        // Or /ckeditor5-[^/]+\/theme\/[^/]+\.css$/ if you want to limit this loader
        // to CKEditor 5 theme only.
        test: /\.css$/,
        use: [
          {
            loader: 'style-loader',
            options: {
              singleton: true
            }
          },
          {
            loader: 'postcss-loader',
            options: styles.getPostCssConfig( {
              themeImporter: {
                themePath: require.resolve( '@ckeditor/ckeditor5-theme-lark' )
              },
              minify: true
            } )
          },
        ]
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
        ]
      }
    ]
  },
  externals: {
    vue: 'Vue',
    vuetify: 'Vuetify',
    jquery: '$',
  },
  optimization: {
    minimize: true,
    minimizer: [
      new TerserPlugin({
        terserOptions: { output: { ascii_only: true } }
      })
    ],
  },
};

module.exports = config;

