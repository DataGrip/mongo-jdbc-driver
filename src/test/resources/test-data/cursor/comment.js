// before
db.col.insert({"_id": 1});
db.col.insert({"_id": 2});
db.col.insert({"_id": 3});
// command
db.col.find().comment("some comment");
// clear
db.col.drop();