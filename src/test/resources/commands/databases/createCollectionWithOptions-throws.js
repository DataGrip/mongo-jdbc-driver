// before
// command
db.createCollection('col', {capped: true, size: 10, unknown: "a"});
// clear
db.getCollection('col').drop();
