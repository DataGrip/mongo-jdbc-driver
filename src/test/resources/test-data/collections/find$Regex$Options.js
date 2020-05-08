// before
db.col.insert({category: "cat1\nString", title: "t1", v: 1});
db.col.insert({category: "cat2\nhello", title: "t3", v: 2});
db.col.insert({category: "cat2", title: "t4", v: 3});
// command
db.col.find({category: {$regex: '^S', $options: 'm'}});
// clear
db.getCollection('col').drop();