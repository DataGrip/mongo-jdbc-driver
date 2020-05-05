// before
db.col.insert({a: 1});
// command
db.createView("v", "col", [], {collation: {numericOrdering: true, alternate: "shifted", locale: "ru"}});
// clear
db.col.drop();
db.v.drop();
