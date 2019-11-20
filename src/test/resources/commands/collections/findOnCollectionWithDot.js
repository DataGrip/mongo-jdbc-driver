// before
db.col.one.two.insert({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.one.two.find();
// clear
db.getCollection('col.one.two').drop();