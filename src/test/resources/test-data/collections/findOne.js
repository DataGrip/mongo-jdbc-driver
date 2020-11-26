// before
db.col.insertOne({key: "value1"});
db.col.insertOne({key: "value2"});
// command
db.col.findOne();
// clear
db.col.drop();