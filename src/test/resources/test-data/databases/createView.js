// before
db.col.insertOne({a: 1});
// command
db.createView("v", "col", [{$project: {_id: 0}}]);
// clear
db.col.drop();
db.v.drop();
