var koa = require('koa.io'),
    app = koa(),
    co = require('co'),
    fs = require('fs'),
    serve = require('koa-static'),
    scjs = require('supercolliderjs'),
    SCAPI = scjs.scapi;
    SCLang = scjs.sclang;

//increments every time we call api
var inc = (function(){
  var count = -1;
  function doInc(){
    count++;
    return count;
  } 
  return doInc;
})();

callSC = {};
co(function *(next){
  var options = yield scjs.resolveOptions();
  var sclang = new SCLang(options);
  yield sclang.boot();
  console.log('booted sclang');
  var sc = new SCAPI(options.host, options.langPort);
  sc.log.dbug(options);
  sc.connect();
  callSC = function(uri, param){
    try{
      var res = sc.call(inc(), uri, param);
      return res;
    }catch(err){
      sc.log.err(err);
    }
  }
  yield callSC('server.boot');
  console.log('booted scsynth');
}).catch(function(err){console.error(err.stack)});

var appState = {
  parGroupList: [],
  busList: []
};


app.use(serve('../res'));

//init:
app.io.use(function* (next) {
  // on connect
  console.log('client connected to socket');
  //should be able to do sockets.emit but something's not working there...
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
  yield* next;
  // on disconnect
});

//socket event
app.io.route('addSynth', function* (next, req) {
  console.log('addSynth received by server');
  var res = yield callSC('triggerfish.newSynth', [parseInt(req.nodeId.toString()), '\default']);
  appState.parGroupList[req.index].synthList.push({index: appState.parGroupList[req.index].synthList.length, nodeId: res.result});
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
  console.log('addSynth done.');
  console.log(res);
});

app.io.route('addGroup', function* (message) {
  console.log('addGroup received by server');
  var res = yield callSC('triggerfish.newParGroup');
  console.log(res);
  appState.parGroupList.push({index: appState.parGroupList.length, nodeId: res.result, synthList: []});
  // console.log("triggerfish.testReply response " + res);
  this.socket.emit('appState', appState);
  this.broadcast.emit('appState', appState);
});

app.listen(3000);

console.log('listening on port 3000')
