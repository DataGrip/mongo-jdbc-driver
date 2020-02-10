// before
db.col.insert({key: "value1"});
db.col.insert({key: "value2"});
// command
db.col.findOne();
// clear
db.getCollection('col').drop();