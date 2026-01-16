// This file is based on start.js from create-react-app
// Updated for webpack-dev-server v4

process.env.NODE_ENV = 'development';

const path = require('path');
const chalk = require('chalk');
const webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const config = require('./webpack.config');

const port = process.env.PORT || 3000;

function clearConsole() {
  process.stdout.write('\x1bc');
}

async function run() {
  const protocol = process.env.HTTPS === 'true' ? 'https' : 'http';
  
  const compiler = webpack(config);

  // "invalid" event fires when you have changed a file, and Webpack is
  // recompiling a bundle.
  compiler.hooks.invalid.tap('invalid', () => {
    clearConsole();
    console.log('Compiling...');
  });

  // "done" event fires when Webpack has finished recompiling the bundle.
  compiler.hooks.done.tap('done', (stats) => {
    clearConsole();
    const hasErrors = stats.hasErrors();
    const hasWarnings = stats.hasWarnings();
    
    if (!hasErrors && !hasWarnings) {
      console.log(chalk.green('Compiled successfully!'));
      console.log();
      console.log('The app is running at:');
      console.log();
      console.log('  ' + chalk.cyan(protocol + '://localhost:' + port + '/'));
      console.log();
      console.log('Note that the development build is not optimized.');
      console.log('To create a production build, use ' + chalk.cyan('npm run build') + '.');
      console.log();
      return;
    }

    if (hasErrors) {
      console.log(chalk.red('Failed to compile.'));
      console.log();
      const info = stats.toJson();
      info.errors.forEach(error => {
        console.log(error.message || error);
        console.log();
      });
      return;
    }

    if (hasWarnings) {
      console.log(chalk.yellow('Compiled with warnings.'));
      console.log();
      const info = stats.toJson();
      info.warnings.forEach(warning => {
        console.log(warning.message || warning);
        console.log();
      });
    }
  });

  const devServerOptions = {
    static: {
      directory: path.join(__dirname, 'public'),
    },
    hot: true,
    historyApiFallback: true,
    port: port,
    https: protocol === 'https',
    client: {
      logging: 'none',
      overlay: true,
    },
  };

  const server = new WebpackDevServer(devServerOptions, compiler);

  console.log(chalk.cyan('Starting the development server...'));
  console.log();

  await server.start();
}

run().catch(err => {
  console.log(chalk.red('Failed to start development server:'));
  console.log(err);
  process.exit(1);
});
