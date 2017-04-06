# MongoDb JDBC Driver

This is an open source JDBC Driver for MongoDb used inside [DbSchema](http://www.dbschema.com) Database Designer.
DbSchema is an interactive diagram designer with relational data explorer, visual query builder, random data generator, forms and reports and many other tools.

## Download Binary

[Available here](http://www.dbschema.com/jdbc-drivers/MongoDbJdbcDriver.zip). Unpack and include all jars in your classpath. 
You can test the driver by simply downloading [DbSchema Tool](http://www.dbschema.com) - it includes a trial period. The driver is included in the software package.

## Description

The driver implements a PreparedStatement where native MongoDb queries can be passed ( db.myCollection.find() ).
The result will be returned as ResultSet with one column containing the JSon object as java.util.Map.

The driver is using the Nashorn JS engine to execute the queries and map them to the objects from the native MongoDb Java driver.
Like we pass to Nashorn a query like : db.myCollection.find({name:'John')) and we pass a binding with db=currentDatabase.
We also wrap MongoDatabase in a JMongoDatabase object for accepting method calls with Map as arguments.
Other objects like MongoCollection are wrapped as well.


```
#!java

import java.sql.Connection;
import java.sql.PreparedStatement;

...

Class.forName("com.dbschema.CassandraJdbcDriver");
Properties properties = new Properties();
properties.put("user", "someuser");
properties.put("password", "somepassword" );
Connection con = DriverManager.getConnection("mongodb://host1:9160/keyspace1", properties);
// OTHER URL (SAME AS FOR MONGODB NATIVE DRIVER): mongodb://db1.example.net,db2.example.net:2500/?replicaSet=test&connectTimeoutMS=300000
String query = "db.sampleCollection().find()";
Statement statement = con.createStatement();
ResultSet rs = statement.executeQuery( query );
Object json = rs.getObject(1);

```

Any contributions to this project are welcome.
We are looking forward to improve this and make possible to execute all MongoDb native queries via JDBC.