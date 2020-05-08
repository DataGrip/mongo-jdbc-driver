// before
db.col.insertOne({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.find({array: {$in: [{another_object: "  .$# "}]}});
// clear
db.getCollection('col').drop();