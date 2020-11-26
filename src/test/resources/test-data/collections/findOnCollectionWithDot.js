// before
db.col.one.two.insertOne({key: "value", array: [1, 2, 3, {another_object: "  .$# "}]});
// command
db.col.one.two.find();
// clear
db.col.one.two.drop();