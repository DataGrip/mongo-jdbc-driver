// command
db.col.bulkWrite([{insertOne: {document: {a: 1}}},{updateOne: {filter: {a: 1}, update: {}, upsert: true}}]);
// clear
db.getCollection('col').drop();