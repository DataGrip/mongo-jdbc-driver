=== MongoDb JDBC Driver ===

This is an open source JDBC Driver for MongoDb used inside [DbSchema Database Designer | http://www.dbschema.com]
Using this driver we make possible to present MongoDb interactive diagrams, relational data explorer, visual query builder, random data generator, forms and reports and many other tools.

The driver implements a PreparedStatement where native MongoDb queries can be passed ( db.myCollection.find() ).
The result will be returned as ResultSet with one column containing the Map JSon object.

The driver is using the Nashorn JS engine to execute the queries and map them to the objects from the native MongoDb Java driver.
Like we pass to Nashorn a query like : db.myCollection.find({name:'John')) and we pass a binding with db=currentDatabase.
We also wrap MongoDatabase in a JMongoDatabase object for accepting method calls with Map as arguments.
Other objects like MongoCollection are wrapped as well.

Any contributions to this project are welcome.
We are looking forward to improve this and make possible to execute all MongoDb native queries via JDBC.