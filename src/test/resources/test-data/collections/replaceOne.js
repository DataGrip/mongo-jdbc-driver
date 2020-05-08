// before
db.col.insert({name: "value1", v: 1});
db.col.insert({name: "value2", v: 2});
db.col.insert({name: "value2", v: 3});
// command
db.col.replaceOne({name: "value1"}, {name: "replacement"});
// clear
db.getCollection('col').drop();