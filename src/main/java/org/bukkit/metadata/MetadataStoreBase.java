package org.bukkit.metadata;

import org.apache.commons.lang.Validate;
import org.bukkit.plugin.Plugin;

import java.util.*;

public abstract class MetadataStoreBase<T> {
    private Map<String, List<MetadataValue>> metadataMap = new HashMap<String, List<MetadataValue>>();
    private WeakHashMap<T, Map<String, String>> disambiguationCache = new WeakHashMap<T, Map<String, String>>();
    private Map<String, MetadataProvider<T>> providers = new HashMap<String, MetadataProvider<T>>();

    /**
     * Adds a metadata value to an object. Each metadata value is owned by a specific{@link Plugin}.
     * If a plugin has already added a metadata value to an object, that value
     * will be replaced with the value of {@code newMetadataValue}. Multiple plugins can set independent values for
     * the same {@code metadataKey} without conflict.
     * <p/>
     * Implementation note: I considered using a {@link java.util.concurrent.locks.ReadWriteLock} for controlling
     * access to {@code metadataMap}, but decided that the added overhead wasn't worth the finer grained access control.
     * Bukkit is almost entirely single threaded so locking overhead shouldn't pose a problem.
     *
     * @param subject          The object receiving the metadata.
     * @param metadataKey      A unique key to identify this metadata.
     * @param newMetadataValue The metadata value to apply.
     * @see MetadataStore#setMetadata(Object, String, MetadataValue)
     * @throws IllegalArgumentException If value is null, or the owning plugin is null
     */
    public synchronized void setMetadata(T subject, String metadataKey, MetadataValue newMetadataValue) {
        Validate.notNull(newMetadataValue, "Value cannot be null");
        Validate.notNull(newMetadataValue.getOwningPlugin(), "Plugin cannot be null");
        String key = cachedDisambiguate(subject, metadataKey);
        if (!metadataMap.containsKey(key)) {
            metadataMap.put(key, new ArrayList<MetadataValue>());
        }
        // we now have a list of subject's metadata for the given metadata key. If newMetadataValue's owningPlugin
        // is found in this list, replace the value rather than add a new one.
        List<MetadataValue> metadataList = metadataMap.get(key);
        for (int i = 0; i < metadataList.size(); i++) {
            if (metadataList.get(i).getOwningPlugin().equals(newMetadataValue.getOwningPlugin())) {
                metadataList.set(i, newMetadataValue);
                return;
            }
        }
        // we didn't find a duplicate...add the new metadata value
        metadataList.add(newMetadataValue);
    }

    /**
     * Returns all metadata values attached to an object. If multiple plugins
     * have attached metadata, each value will be included.
     *
     * @param subject     the object being interrogated.
     * @param metadataKey the unique metadata key being sought.
     * @return A list of values, one for each plugin that has set the requested value.
     * @see MetadataStore#getMetadata(Object, String)
     */
    public synchronized List<MetadataValue> getMetadata(T subject, String metadataKey) {
       
        String key = cachedDisambiguate(subject, metadataKey);
        if (metadataMap.containsKey(key)) {
            return Collections.unmodifiableList(metadataMap.get(key));
        } else if (providers.containsKey(metadataKey)) {
            if (buildProviderData(subject, metadataKey)) {
                return Collections.unmodifiableList(metadataMap.get(key));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Tests to see if a metadata attribute has been set on an object.
     *
     * @param subject     the object upon which the has-metadata test is performed.
     * @param metadataKey the unique metadata key being queried.
     * @return the existence of the metadataKey within subject.
     */
    public synchronized boolean hasMetadata(T subject, String metadataKey) {
        String key = cachedDisambiguate(subject, metadataKey);
        if (metadataMap.containsKey(key)) return true;
        if (providers.containsKey(metadataKey)) {
            return buildProviderData(subject, metadataKey);
        }
        return false;
    }

    /**
     * Removes a metadata item owned by a plugin from a subject.
     *
     * @param subject      the object to remove the metadata from.
     * @param metadataKey  the unique metadata key identifying the metadata to remove.
     * @param owningPlugin the plugin attempting to remove a metadata item.
     * @see MetadataStore#removeMetadata(Object, String, org.bukkit.plugin.Plugin)
     * @throws IllegalArgumentException If plugin is null
     */
    public synchronized void removeMetadata(T subject, String metadataKey, Plugin owningPlugin) {
        Validate.notNull(owningPlugin, "Plugin cannot be null");
        String key = cachedDisambiguate(subject, metadataKey);
        List<MetadataValue> metadataList = metadataMap.get(key);
        if (metadataList == null) return;
        for (int i = 0; i < metadataList.size(); i++) {
            if (metadataList.get(i).getOwningPlugin().equals(owningPlugin)) {
                metadataList.remove(i);
                if (metadataList.isEmpty()) {
                    metadataMap.remove(key);
                }
            }
        }
    }

    /**
     * Invalidates all metadata in the metadata store that originates from the given plugin. Doing this will force
     * each invalidated metadata item to be recalculated the next time it is accessed.
     *
     * @param owningPlugin the plugin requesting the invalidation.
     * @see MetadataStore#invalidateAll(org.bukkit.plugin.Plugin)
     * @throws IllegalArgumentException If plugin is null
     */
    public synchronized void invalidateAll(Plugin owningPlugin) {
        Validate.notNull(owningPlugin, "Plugin cannot be null");
        for (List<MetadataValue> values : metadataMap.values()) {
            for (MetadataValue value : values) {
                if (value.getOwningPlugin().equals(owningPlugin)) {
                    value.invalidate();
                }
            }
        }
    }

    /**
     * Register a provider to do on-demand metadata.
     * @param metadataKey The key for which this provider is answering for.
     * @param provider A metadata provider.
     * @return true if provider was added, false if there was already a provider.
     */
    public boolean registerProvider(String metadataKey, MetadataProvider<T> provider) {
        Validate.notNull(metadataKey, "metadataKey cannot be null");
        Validate.notNull(provider, "Provider cannot be null");
        Validate.notNull(provider.getOwningPlugin(), "provider.owningPlugin() cannot be null");
        return (providers.put(metadataKey, provider) == null);
    }

    /**
     * Unregister an on-demand provider.
     * @param metadataKey The key for which this provider is answering for.
     * @return true if a provider was removed, false otherwise
     */
    public boolean unregisterProvider(String metadataKey) {
        return (providers.remove(metadataKey) != null);
    }


    /**
     * Caches the results of calls to {@link MetadataStoreBase#disambiguate(Object, String)} in a {@link WeakHashMap}. Doing so maintains a
     * <a href="http://www.codeinstructions.com/2008/09/weakhashmap-is-not-cache-understanding.html">canonical list</a>
     * of disambiguation strings for objects in memory. When those objects are garbage collected, the disambiguation string
     * in the list is aggressively garbage collected as well.
     *
     * @param subject     The object for which this key is being generated.
     * @param metadataKey The name identifying the metadata value.
     * @return a unique metadata key for the given subject.
     */
    private String cachedDisambiguate(T subject, String metadataKey) {
        if (disambiguationCache.containsKey(subject) && disambiguationCache.get(subject).containsKey(metadataKey)) {
            return disambiguationCache.get(subject).get(metadataKey);
        } else {
            if (!disambiguationCache.containsKey(subject)) {
                disambiguationCache.put(subject, new HashMap<String, String>());
            }
            String disambiguation = disambiguate(subject, metadataKey);
            disambiguationCache.get(subject).put(metadataKey, disambiguation);
            return disambiguation;
        }
    }

    /**
     * Creates a unique name for the object receiving metadata by combining unique data from the subject with a metadataKey.
     * The name created must be globally unique for the given object and any two equivalent objects must generate the
     * same unique name. For example, two Player objects must generate the same string if they represent the same player,
     * even if the objects would fail a reference equality test.
     *
     * @param subject     The object for which this key is being generated.
     * @param metadataKey The name identifying the metadata value.
     * @return a unique metadata key for the given subject.
     */
    protected abstract String disambiguate(T subject, String metadataKey);
    
    
    /**
     * Retrieve provider data for this key, and set it if it's not null.
     * @param subject The context for which we're getting this metadata
     * @param metadataKey The key on which we're answering
     * @return true if we got provider data, false otherwise.
     */
    private boolean buildProviderData(T subject, String metadataKey) {
        MetadataValue providedValue = providers.get(metadataKey).getValue(subject, metadataKey);
        if (providedValue != null) {
            setMetadata(subject, metadataKey, providedValue);
            return true;
        }
        return false;
    }
}
