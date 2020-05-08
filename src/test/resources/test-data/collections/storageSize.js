// before
db.col.insert({key: "value"});
// command
db.col.storageSize();
// clear
db.getCollection('col').drop();