// before
// command
db.createCollection('col', {capped: true, size: 10, ignored: "a"});
// clear
db.getCollection('col').drop();
