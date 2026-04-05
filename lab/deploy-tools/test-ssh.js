const { Client } = require('ssh2');

const password = 'Cjh041217@';
const conn = new Client();

conn
  .on('keyboard-interactive', (_name, _instructions, _lang, prompts, finish) => {
    console.log('keyboard-interactive', prompts.map((prompt) => prompt.prompt));
    finish(prompts.map(() => password));
  })
  .on('ready', () => {
    console.log('SSH ready');
    conn.exec('echo ok', (err, stream) => {
      if (err) {
        console.error(err);
        conn.end();
        process.exitCode = 1;
        return;
      }
      let stdout = '';
      let stderr = '';
      stream.on('close', (code) => {
        console.log('exit', code);
        if (stdout) process.stdout.write(stdout);
        if (stderr) process.stderr.write(stderr);
        conn.end();
      });
      stream.on('data', (data) => {
        stdout += data.toString();
      });
      stream.stderr.on('data', (data) => {
        stderr += data.toString();
      });
    });
  })
  .on('error', (err) => {
    console.error('SSH error:', err);
    process.exit(1);
  })
  .connect({
    host: '101.35.79.76',
    port: 22,
    username: 'ubuntu',
    password,
    tryKeyboard: true,
    readyTimeout: 300000
  });
