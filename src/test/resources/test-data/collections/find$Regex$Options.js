// before
db.col.insertOne({category: "cat1\nString", title: "t1", v: 1});
db.col.insertOne({category: "cat2\nhello", title: "t3", v: 2});
db.col.insertOne({category: "cat2", title: "t4", v: 3});
// command
db.col.find({category: {$regex: '^S', $options: 'm'}});
// clear
db.col.drop();