// before
db.col.insertOne({key: "value", v: 1});
// command
db.col.findOneAndUpdate({key: "value"}, {$inc: {v: 1}}, {returnNewDocument: true});
// clear
db.col.drop();