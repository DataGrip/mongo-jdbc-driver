// before
use testdb;
db.col.insertOne({key: "value"});
// command
db.col.copyTo("newCol");
// command
db.newCol.find();
// clear
db.col.drop();
db.newCol.drop();