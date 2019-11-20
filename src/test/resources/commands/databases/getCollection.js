// before
db.col.insert({a: 1});
// command
db.getCollection('col');
// clear
db.getCollection('col').drop();
