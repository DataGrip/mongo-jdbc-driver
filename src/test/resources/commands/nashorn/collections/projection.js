// before
db.col.insert({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.find({}, {_id: 0});
// clear
db.getCollection('col').drop();