// before
db.col.insert({key: "value"});
// command
db.col.dataSize();
// clear
db.getCollection('col').drop();