# MongoDb JDBC Driver

This is an open source JDBC Driver for MongoDb used inside [DbSchema](http://www.dbschema.com) Database Designer.
DbSchema is an interactive diagram designer with relational data explorer, visual query builder, random data generator, forms and reports and many other tools.

## Download Binary

[Available here](http://www.dbschema.com/jdbc-drivers/MongoDbJdbcDriver.zip). Unpack and include all jars in your classpath. 
You can test the driver by simply downloading [DbSchema Database Designer](http://www.dbschema.com). The driver is included in the software package.

## Description

The driver implements a PreparedStatement where native MongoDb queries can be passed ( db.myCollection.find() ).
The result will be returned as ResultSet with one column containing the JSon object as java.util.Map.

The driver is using the Nashorn JS engine to execute the queries and map them to the objects from the native MongoDb Java driver.
Like we pass to Nashorn a query like : db.myCollection.find({name:'John')) and we pass a binding with db=currentDatabase.
We also wrap MongoDatabase in a JMongoDatabase object for accepting method calls with Map as arguments.
Other objects like MongoCollection are wrapped as well.

Any contributions to this project are welcome.
We are looking forward to improve this and make possible to execute all MongoDb native queries via JDBC.