// before
db.orders.insert({
    _id: ObjectId("50a8240b927d5d8b5891743c"),
    cust_id: "abc123",
    ord_date: new Date("Oct 04, 2012"),
    status: 'A',
    price: 25,
    items: [ { sku: "mmm", qty: 5, price: 2.5 },
        { sku: "nnn", qty: 5, price: 2.5 } ]
});

var mapFunction1 = function() { emit(this.cust_id, this.price); };
var reduceFunction1 = function(keyCustId, valuesPrices) { return Array.sum(valuesPrices); };
// command
db.orders.mapReduce( mapFunction1, reduceFunction1 );
// clear
db.orders.drop();