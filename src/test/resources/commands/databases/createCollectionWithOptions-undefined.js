// before
// command
db.createCollection('col', {capped: true, size: 10});
// clear
db.getCollection('col').drop();
