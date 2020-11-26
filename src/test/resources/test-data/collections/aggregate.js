// before
db.col.insertOne({name: "value1", v: 1});
db.col.insertOne({name: "value2", v: 2});
db.col.insertOne({name: "value2", v: 3});
// command
db.col.aggregate([{$group: {_id: "$name", total: {$sum: "$v"}}}, {$sort: {total: -1}}]);
// clear
db.col.drop();