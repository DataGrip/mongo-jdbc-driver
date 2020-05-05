// before
db.col.insert({key: "value"});
// command
db.col.totalIndexSize();
// clear
db.getCollection('col').drop();