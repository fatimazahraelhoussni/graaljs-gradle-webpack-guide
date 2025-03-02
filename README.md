
### Using Node Packages in a Java Application with Gradle

JavaScript libraries can be packaged with Java applications, and this integration is facilitated through GraalJS and the GraalVM Polyglot API. This guide shows you how to use the `qrcode` NPM package in a Java application, using Gradle as the build tool.

#### 1. Getting Started
In this guide, you will add the `qrcode` NPM package to a Java application to generate QR codes.

##### Prerequisites:
- JDK 21 or later
- Gradle 7.0 or later
- A text editor or IDE

#### 2. Setting Up the Gradle Project
You can start with any Gradle-based Java application. For this guide, generating a simple Gradle project is enough.

To generate a new project:
```bash
gradle init --type java-application
cd qrdemo
```

##### 2.1 Adding the GraalJS Dependencies
Add the required GraalJS dependencies in your `build.gradle` file:

```gradle
dependencies {
    implementation 'org.graalvm.polyglot:polyglot:24.1.2'  // Graal Polyglot API
    implementation 'org.graalvm.polyglot:js:24.1.2'      // GraalJS
}
```

This will provide the necessary GraalJS setup to use JavaScript code in a Java application.

##### 2.2 Adding the Node.js and NPM Management Plugin
You can use the Gradle Node plugin to manage the installation of Node.js and NPM, and to run webpack for bundling JavaScript dependencies.

Add the Node plugin to your `build.gradle` file:

```gradle
plugins {
    id 'com.github.node-gradle.node' version '3.1.0'
}

node {
    version = '21.7.2'  // Specify the Node.js version
    npmVersion = '8.19.2'  // Specify the NPM version
    download = true  // Download Node.js and NPM
}

task buildJS(type: NpmTask) {
    dependsOn npmInstall
    description = "Build JavaScript assets"
    args = ['run', 'build']
}

task installNodeModules(type: NpmTask) {
    args = ['install']
}

build.dependsOn buildJS
```

This configuration installs Node.js and npm, then runs the JavaScript build through webpack.

#### 3. Setting Up the JavaScript Build
Create the `src/main/js` directory in your project and set up the JavaScript build environment:

1. Initialize the NPM project:
   ```bash
   mkdir -p src/main/js
   cd src/main/js
   npm init -y
   ```

2. Install the necessary packages:
   ```bash
   npm install qrcode
   npm install --save @webpack-cli/generators
   npm install --save assert util stream-browserify browserify-zlib fast-text-encoding
   ```

3. Create the webpack configuration file `webpack.config.js`:

```javascript
const path = require('path');
const { EnvironmentPlugin } = require('webpack');

module.exports = {
    entry: './main.mjs',
    output: {
        path: path.resolve(process.env.BUILD_DIR),
        filename: 'bundle.mjs',
        module: true,
        library: { type: 'module' },
        globalObject: 'globalThis',
    },
    experiments: {
        outputModule: true,
    },
    optimization: {
        usedExports: true,
        minimize: false,
    },
    resolve: {
        fallback: {
            "stream": require.resolve("stream-browserify"),
            "zlib": require.resolve("browserify-zlib"),
            "fs": false,
        },
    },
    plugins: [
        new EnvironmentPlugin({
            NODE_DEBUG: false,
        }),
    ],
};
```

4. Create the `main.mjs` entry file:

```javascript
import 'fast-text-encoding'; // Polyfill for TextEncoder
export * as QRCode from 'qrcode';
```

5. Add the build script to your `package.json`:

```json
{
  "name": "qrdemo",
  "version": "1.0.0",
  "scripts": {
    "build": "webpack --mode=production --node-env=production"
  },
  "dependencies": {
    "qrcode": "^1.5.4",
    "assert": "^2.1.0",
    "stream-browserify": "^3.0.0",
    "browserify-zlib": "^0.2.0",
    "fast-text-encoding": "^1.0.6",
    "util": "^0.12.5"
  },
  "devDependencies": {
    "@webpack-cli/generators": "^3.0.7",
    "webpack": "^5.94.0",
    "webpack-cli": "^5.1.4"
  }
}
```

#### 4. Using the JavaScript Library in Java
In your Java code, you can now use GraalJS to call the `QRCode` module from the generated `bundle.mjs`.

```java
package com.example;

import org.graalvm.polyglot.*;

public class App {
    public static void main(String[] args) throws Exception {
        try (Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false")
                .option("js.esm-eval-returns-exports", "true")
                .option("js.unhandled-rejections", "throw")
                .build()) {

            Source bundleSrc = Source.newBuilder("js", App.class.getResource("/bundle/bundle.mjs")).build();
            Value exports = context.eval(bundleSrc);

            QRCode qrCode = exports.getMember("QRCode").as(QRCode.class);
            String input = args.length > 0 ? args[0] : "https://www.graalvm.org/";

            Promise resultPromise = qrCode.toString(input);
            resultPromise.then((Value output) -> {
                System.out.println("Successfully generated QR code for \"" + input + "\".");
                System.out.println(output.asString());
            });
        }
    }
}
```

#### 5. Running the Application
To build and run your application, use the following commands:

```bash
gradle build
gradle run --args="https://www.graalvm.org/"
```

You should see the generated QR code as output.

### Conclusion
This guide demonstrated how to use Node.js packages like `qrcode` within a Java application using GraalVM and Gradle. With GraalJS, you can easily integrate JavaScript libraries into Java, allowing for powerful polyglot applications.
