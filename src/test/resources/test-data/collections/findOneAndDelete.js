// before
db.col.insertOne({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.findOneAndDelete({key: "value"});
// clear
db.col.drop();