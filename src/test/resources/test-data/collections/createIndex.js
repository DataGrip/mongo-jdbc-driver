// before
db.col.insert({category: "cat1", v: 1});
db.col.insert({category: "cat2", v: 2});
db.col.insert({category: "cat2", v: 3});
// command
db.col.createIndex({category: 1}, {collation: {locale: "fr"}});
// clear
db.col.drop();