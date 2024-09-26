package hibiscus.cetide.app.basic.signal;
import java.util.*;

import static hibiscus.cetide.app.basic.signal.Signals.sig;

public class Main {
    public static void main(String[] args) {
        // 示例用法
        Signals signals = sig();
        signals.connect("test", (sender, params) -> System.out.println("Received from " + sender + ": " + Arrays.toString(params)));
        signals.emit("test", "Emitter", "Hello", "World");
    }
}
