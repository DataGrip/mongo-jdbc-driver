// before
db.col.insert({key: "value"});
db.col.aggregate({$out: "newCol"});
// command
db.newCol.find();
// clear
db.getCollection('col').drop();
db.getCollection('newCol').drop();