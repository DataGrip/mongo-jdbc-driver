// before
db.col.insert({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.findOneAndReplace({key: "value"}, {key: "newValue"}, {projection: {_id: 0}, returnNewDocument: true});
// clear
db.col.drop();