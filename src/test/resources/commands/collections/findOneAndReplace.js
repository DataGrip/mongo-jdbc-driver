// before
db.col.insert({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.findOneAndReplace({key: "value"}, {key: "newValue"});
// clear
db.getCollection('col').drop();