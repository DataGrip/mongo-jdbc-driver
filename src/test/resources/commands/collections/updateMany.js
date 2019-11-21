// before
db.restaurant.insertMany([
    {"_id": 1, "name": "Central Perk Cafe", "violations": 3},
    {"_id": 2, "name": "Rock A Feller Bar and Grill", "violations": 2},
    {"_id": 3, "name": "Empire State Sub", "violations": 5},
    {"_id": 4, "name": "Pizza Rat's Pizzaria", "violations": 8}
]);
// command
db.restaurant.updateMany(
    {violations: {$gt: 4}},
    {$set: {"Review": true}}
);
// clear
db.getCollection('restaurant').drop();