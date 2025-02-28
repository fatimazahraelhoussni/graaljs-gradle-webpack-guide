package org.example;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class App {
    public App() {
    }

    public static void main(String[] args) throws Exception {
        try (Context context = Context.newBuilder(new String[]{"js"})
                .allowHostAccess(HostAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false")
                .option("js.esm-eval-returns-exports", "true")
                .option("js.unhandled-rejections", "throw")
                .build()) {

            Path bundlePath = Paths.get("build/classes/bundle/bundle.mjs");
            if (!bundlePath.toFile().exists()) {
                throw new RuntimeException("Bundle file bundle.mjs not found at location: " + bundlePath);
            }

            Source bundleSrc = Source.newBuilder("js", bundlePath.toFile()).build();
            Value exports = context.eval(bundleSrc);

            String input = args.length > 0 ? args[0] : "https://www.graalvm.org/javascript/";
            QRCode qrCode = exports.getMember("QRCode").as(QRCode.class);
            Promise resultPromise = qrCode.toString(input);

            resultPromise.then((result) -> {
                System.out.println("Successfully generated QR code for \"" + input + "\".");
                System.out.println(result.asString());
            });

            Value qrCodeValue = exports.getMember("QRCode");
            Value resultValue = qrCodeValue.invokeMember("toString", input);

            resultValue.invokeMember("then", (ProxyExecutable) (arguments) -> {
                Value result = arguments[0];
                System.out.println("Successfully generated QR code for \"" + input + "\".");
                System.out.println(result.asString());
                return result;
            });
        }
    }
}
