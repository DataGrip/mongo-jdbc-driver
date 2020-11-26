// before
db.col.insertOne({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.findOneAndReplace({key: "value"}, {key: "newValue"});
// clear
db.col.drop();