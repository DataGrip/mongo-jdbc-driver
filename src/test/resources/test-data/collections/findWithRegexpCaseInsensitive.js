// before
db.col.drop();
db.col.insert({category: "cat1", title: "t1", v: 1});
db.col.insert({category: "cat2", title: "t3", v: 2});
db.col.insert({category: "cat2", title: "t4", v: 3});
// command
db.col.find({category: /Cat./i});
// clear
db.col.drop();