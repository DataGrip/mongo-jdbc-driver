// before
db.col.insert({name: "value1", v: 1});
db.col.insert({name: "value2", v: 2});
db.col.insert({name: "value2", v: 3});
// command
db.col.estimatedDocumentCount({maxTimeMS: 1000});
// clear
db.col.drop();