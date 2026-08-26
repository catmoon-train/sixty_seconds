package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.GlobalTodo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 修复 LostCities 的 GlobalTodo 崩溃
 */
@Mixin(GlobalTodo.class)
public abstract class GlobalTodoMixin {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "java/util/HashMap"))
    private HashMap<Object, Object> sixtySecondsUseConcurrentTodoMap() {
        // 注意：返回类型必须是 HashMap，所以这里返回委托到 ConcurrentHashMap 的子类。
        @SuppressWarnings("unchecked")
        HashMap<Object, Object> map = (HashMap<Object, Object>) (Map<Object, Object>) new ConcurrentHashMapWrapper<>();
        return map;
    }

    /**
     * 内部委托给 ConcurrentHashMap 的 HashMap 子类。
     * 仅用于替换 GlobalTodo 中 todoQueues 字段持有的 Map 实例。
     */
    private static final class ConcurrentHashMapWrapper<K, V> extends HashMap<K, V> {
        private final ConcurrentHashMap<K, V> delegate = new ConcurrentHashMap<>();
        private static final long serialVersionUID = 1L;

        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean containsKey(Object key) { return delegate.containsKey(key); }
        @Override public boolean containsValue(Object value) { return delegate.containsValue(value); }
        @Override public V get(Object key) { return delegate.get(key); }
        @Override public V getOrDefault(Object key, V defaultValue) { return delegate.getOrDefault(key, defaultValue); }
        @Override public V put(K key, V value) { return delegate.put(key, value); }
        @Override public void putAll(Map<? extends K, ? extends V> m) { delegate.putAll(m); }
        @Override public V remove(Object key) { return delegate.remove(key); }
        @Override public boolean remove(Object key, Object value) { return delegate.remove(key, value); }
        @Override public V replace(K key, V value) { return delegate.replace(key, value); }
        @Override public boolean replace(K key, V oldValue, V newValue) { return delegate.replace(key, oldValue, newValue); }
        @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) { delegate.replaceAll(function); }
        @Override public V putIfAbsent(K key, V value) { return delegate.putIfAbsent(key, value); }
        @Override public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) { return delegate.merge(key, value, remappingFunction); }
        @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) { return delegate.computeIfAbsent(key, mappingFunction); }
        @Override public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return delegate.compute(key, remappingFunction); }
        @Override public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return delegate.computeIfPresent(key, remappingFunction); }
        @Override public void forEach(BiConsumer<? super K, ? super V> action) { delegate.forEach(action); }
        @Override public void clear() { delegate.clear(); }
        @Override public Set<K> keySet() { return delegate.keySet(); }
        @Override public Set<Map.Entry<K, V>> entrySet() { return delegate.entrySet(); }
        @Override public java.util.Collection<V> values() { return delegate.values(); }
    }
}
