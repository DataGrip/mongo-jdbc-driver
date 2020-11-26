// before
db.col.insertOne({"_id": 1});
db.col.insertOne({"_id": 2});
db.col.insertOne({"_id": 3});
// command
db.col.find().comment("some comment");
// clear
db.col.drop();