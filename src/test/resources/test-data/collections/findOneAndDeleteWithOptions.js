// before
db.col.insertOne({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.findOneAndDelete({key: "value"}, {projection: {_id: 0}});
// clear
db.col.drop();