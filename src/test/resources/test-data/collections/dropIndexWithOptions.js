// before
db.col.insertOne({category: "cat1", v: 1});
db.col.insertOne({category: "cat2", v: 2});
db.col.insertOne({category: "cat2", v: 3});
db.col.createIndex({category: 1}, {collation: {locale: "fr"}});
// command
db.col.dropIndex({category: 1});
// clear
db.col.drop();