// before
db.col.insertOne({a: 1});
db.createView("v", "col", [{$project: {_id: 0}}]);
// command
db.v.find();
// clear
db.col.drop();
db.v.drop();
