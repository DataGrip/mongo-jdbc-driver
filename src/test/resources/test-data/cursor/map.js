// before
db.col.insertOne({"_id": 1, name: "Vasya"});
db.col.insertOne({"_id": 2, name: "Petya"});
db.col.insertOne({"_id": 3, name: "Lyusya"});
// command
db.col.find().map( function(u) { return u.name; } );
// clear
db.col.drop();