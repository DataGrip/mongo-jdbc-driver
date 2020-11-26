// before
db.col.insertOne({"_id": 1, "x": "a"});
db.col.insertOne({"_id": 2, "x": "A"});
db.col.insertOne({"_id": 3, "x": "á"});
// command
db.col.find({x: "a"}).collation({locale: "en_US", strength: 1});
// clear
db.col.drop();