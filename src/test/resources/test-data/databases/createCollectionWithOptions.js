// before
// command
db.createCollection('col', {capped: true, size: 10});
// command
db.createCollection('col', {capped: true, size: 10, unknown: "a"});
// clear
db.col.drop();
