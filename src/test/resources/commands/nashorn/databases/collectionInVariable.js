// before
db.col.insert({a: 1});
var col = db.getCollection('col');
// command
col;
// clear
db.getCollection('col').drop();
