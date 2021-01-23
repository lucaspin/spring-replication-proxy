const http = require('http').Server();
const fs = require('fs');
const io = require('socket.io')(http);

io.on('connection', (socket) => {
  console.log('A client connected');
  const wstream = fs.createWriteStream('/tmp/audio-from-ffmpeg');

  socket.on('disconnect', () => {
    console.log('A client disconnected');
    wstream.end();
  });

  socket.on('message', msg => {
    wstream.write(Buffer.from(msg));
  });
});

http.listen(4010, () => {
  console.log('listening on *:4010');
});