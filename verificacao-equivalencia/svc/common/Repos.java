import br.edu.foodnow.model.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Repos {
    static final AtomicLong SEQ = new AtomicLong(100);

    static void assign(Object o, Set<Object> seen) throws Exception {
        if (o == null || !seen.add(new IdentityKey(o))) return;
        Class<?> c = o.getClass();
        if (!c.getPackageName().startsWith("br.edu.foodnow.model") || c.isEnum() || c == Localizacao.class) return;
        for (Field f : c.getDeclaredFields()) {
            f.setAccessible(true);
            if (f.getName().equals("id") && f.get(o) == null) f.set(o, SEQ.incrementAndGet());
            Object v = f.get(o);
            if (v instanceof Collection<?> col) for (Object e : col) assign(e, seen);
            else if (v != null && v.getClass().getPackageName().startsWith("br.edu.foodnow.model")) assign(v, seen);
        }
    }
    record IdentityKey(Object o) { public boolean equals(Object x) { return x instanceof IdentityKey k && k.o == o; } public int hashCode() { return System.identityHashCode(o); } }

    static Long idOf(Object o) throws Exception { Field f = o.getClass().getDeclaredField("id"); f.setAccessible(true); return (Long) f.get(o); }

    @SuppressWarnings("unchecked")
    public static <T> T repo(Class<T> iface) {
        Map<Long, Object> store = new LinkedHashMap<>();
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[]{iface}, (proxy, m, args) -> {
            switch (m.getName()) {
                case "findById": return Optional.ofNullable(store.get(args[0]));
                case "save": assign(args[0], new HashSet<>()); store.put(idOf(args[0]), args[0]); return args[0];
                case "findByPedidoId": return store.values().stream().filter(e -> ((Entrega) e).getPedido().getId().equals(args[0])).findFirst();
                default: throw new UnsupportedOperationException(m.getName());
            }
        });
    }
}
