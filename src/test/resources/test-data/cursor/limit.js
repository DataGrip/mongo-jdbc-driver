// before
db.col.insertOne({"_id": 1});
db.col.insertOne({"_id": 2});
db.col.insertOne({"_id": 3});
// command
db.col.find().limit(2);
// clear
db.col.drop();