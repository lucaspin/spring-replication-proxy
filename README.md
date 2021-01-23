# Replicating proxy

This is a simple example of how to use spring to create an RTP receiver.
In this case, the packets received are sent through a Websocket connection.

## Running

1. Run the websocket server

```
cd ws-server
npm install
node index.js
```

2. Compile and run the RTP receiver

```
mvn clean install -DskipTests
java -jar target/replicating-proxy-0.0.1-SNAPSHOT.jar
```

3. Allocate a port and create an RTP stream with ffmpeg

```
curl -X POST http://localhost:8080/ports
ffmpeg \
    -re \
    -i media/pcm_s16le-44100hz-s16-10s.wav \
    -c:a copy \
    -f rtp \
    "rtp://127.0.0.1:11111"
```

4. Once the ffmpeg command is done, check that the media was correctly replicated

```
mplayer /tmp/audio-from-ffmpeg
```
