// before
db.col.insertOne({category: "cat1", title: "t1", v: 1});
db.col.insertOne({category: "cat2", title: "t3", v: 2});
db.col.insertOne({category: "cat2", title: "t4", v: 3});
// command
db.col.find({category: {$regex: 'cat.'}});
// clear
db.col.drop();