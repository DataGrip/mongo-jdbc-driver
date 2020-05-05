// before
db.col.insert({key: "value", v: 1});
// command
db.col.findOneAndUpdate({key: "value"}, {$inc: {v: 1}}, {returnNewDocument: true});
// clear
db.getCollection('col').drop();