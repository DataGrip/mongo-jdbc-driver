// before
db.col.insertOne({category: "cat1", title: "t1", v: 1});
db.col.insertOne({category: "cat2", title: "t3", v: 2});
db.col.insertOne({category: "cat2", title: "t4", v: 3});
// command
db.col.createIndexes([{"category": 1}, {"title": 1}], {collation: {locale: "fr", strength: 2}});
// clear
db.col.drop();