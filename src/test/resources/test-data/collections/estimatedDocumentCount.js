// before
db.col.insertOne({name: "value1", v: 1});
db.col.insertOne({name: "value2", v: 2});
db.col.insertOne({name: "value2", v: 3});
// command
db.col.estimatedDocumentCount({maxTimeMS: 1000});
// clear
db.col.drop();