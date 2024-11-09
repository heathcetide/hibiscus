package hibiscus.cetide.app.basic.signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 实现信号管理器类
public class Signals {

    // 单例模式获取Signals实例
    private static final Signals SIG = new Signals();

    public static Signals sig() {
        return SIG;
    }

    private int lastID = 0;
    private Map<String, List<SigHandler>> sigHandlers = new HashMap<>();
    private boolean inLoop = false;
    private List<SigHandlerEvent> events = new ArrayList<>();

    public Signals() {}

    public synchronized void processEvents() {
        if (events.isEmpty() || inLoop) return;

        try {
            for (SigHandlerEvent event : events) {
                List<SigHandler> sigs = sigHandlers.getOrDefault(event.getSignalName(), new ArrayList<>());

                switch (event.getEvType()) {
                    case 0: // evTypeAdd
                        sigs.add(event.getSigHandler());
                        break;
                    case 1:
                        sigs.removeIf(sh -> sh.getId() == event.getSigHandler().getId());
                        break;
                }

                sigHandlers.put(event.getSignalName(), sigs);
            }
        } finally {
            events.clear();
        }
    }

    public int connect(String event, SignalHandler handler) {
        lastID++;
        SigHandler sigHandler = new SigHandler(lastID, handler);
        SigHandlerEvent ev = new SigHandlerEvent(0, event, sigHandler);
        events.add(ev);
        processEvents();
        return lastID;
    }

    public void disconnect(String event, int id) {
        SigHandlerEvent ev = new SigHandlerEvent(1, event, new SigHandler(id, null));
        events.add(ev);
        processEvents();
    }

    public void clear(String... events) {
        for (String event : events) {
            this.sigHandlers.remove(event);
        }
    }

    public void emit(String event, Object sender, Object... params) {
        inLoop = true;
        try {
            List<SigHandler> sigs = sigHandlers.get(event);
            if (sigs != null) {
                for (SigHandler sig : sigs) {
                    sig.getHandler().handle(sender, params);
                }
            }
        } finally {
            inLoop = false;
            processEvents();
        }
    }
}