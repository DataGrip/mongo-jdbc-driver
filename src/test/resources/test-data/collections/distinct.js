// before
db.col.insertMany([{v: 1}, {v: 2}, {v: 3}, {v: "hello"}, {v: 1}]);
// command
db.col.distinct("v");
// clear
db.col.drop();