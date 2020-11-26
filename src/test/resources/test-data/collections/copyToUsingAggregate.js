// before
db.col.insertOne({key: "value"});
db.col.aggregate({$out: "newCol"});
// command
db.newCol.find();
// clear
db.col.drop();
db.newCol.drop();