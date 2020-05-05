// before
db.col.insert({"_id": 1});
db.col.insert({"_id": 2});
db.col.insert({"_id": 3});
// command
db.col.find().limit(2);
// clear
db.getCollection('col').drop();