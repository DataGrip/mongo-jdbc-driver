# MongoDB JDBC Driver

[![Apache licensed](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](./license.txt)
[![Latest Release](https://img.shields.io/github/v/release/datagrip/mongo-jdbc-driver?label=latest)](https://github.com/DataGrip/mongo-jdbc-driver/releases/tag/v1.20)

Type 4 JDBC driver that allows Java programs to connect to a MongoDB database.

This driver is embedded into [DataGrip](https://www.jetbrains.com/datagrip/features/mongodb/).

### Download

You can download the precompiled driver (jar) on the [releases page](https://github.com/DataGrip/mongo-jdbc-driver/releases).

### Build

```
# Linux, MacOs
./gradlew shadowJar

# Windows
gradlew.bat shadowJar
```

You will find driver (jar) in ```build/libs```.
