// before
db.col.insert({key: "value1", a: 1});
db.col.insert({key: "value2", a: 2});
db.col.insert({key: "value3", a: 2});
// command
db.col.findOne({a: 2}, {key: 1});
// clear
db.col.drop();