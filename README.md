# MongoDb JDBC Driver

This is an open source JDBC Driver for MongoDb. We implemented this for use with [DbSchema](http://www.dbschema.com) Tool, but we make it available for everybody who needs a JDBC driver. The driver can execute native MongoDb queries and use the JDBC standards.

About DbSchema: [DbSchema](http://www.dbschema.com)  is an interactive diagram designer with relational data explorer, visual query builder, random data generator, forms and reports and many other tools. You can experiment the driver by downloading DbSchema and connecting to MongoDb - DbSchema has a trial period. The driver is included in the software package.

## Download Binary

[Available here](http://www.dbschema.com/jdbc-drivers/MongoDbJdbcDriver.zip). Unpack and include all jars in your classpath. 

## Driver URL

The driver is using the same URL as [native MongoDb Java driver](https://docs.mongodb.com/manual/reference/connection-string/). In fact the JDBC is using the native driver to connect and implements the JDBC functionality on top of it.

## How it Works

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


## DbSchema Main Features for MongoDb

* Structure discovery and diagrams 
* Relational Data Browse and Editor
* Query Editor
* Visual Query Builder
* Random Data Generator
* Data Loader
 

DbSchema reads sample JSon documents from the database and builds diagrams showing the JSon structure. We consider that each collection documents have similar structure.

![mongodb1.png](https://bitbucket.org/repo/BELRaG/images/282491526-mongodb1.png)

Use the Query Editor to edit and execute MongoDb queries in the native language:

![mongodb2.png](https://bitbucket.org/repo/BELRaG/images/2249668125-mongodb2.png)


Using Relational Data Browse you can explore data from multiple collections simultaneously. 
Collections may bind one with another using virtual relations ( if one field value points to a certain document from another collection ). 
This is shown as a line between collections ( see here master and slave ). T
hen data from both collections can be explored. Clicking a document in the first collection will update the second collection with the matching documents.

![mongo3.png](https://bitbucket.org/repo/BELRaG/images/2228714881-mongo3.png)

A full description of DbSchema features is available on [DbSchema website](http://www.dbschema.com/mongodb-tool.html).