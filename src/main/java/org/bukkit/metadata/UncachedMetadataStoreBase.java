package org.bukkit.metadata;

public abstract class UncachedMetadataStoreBase<T> extends MetadataStoreBase<T> {
	/** Uncached disambiguate to test if performance actually improves with "caching". */
	protected String cachedDisambiguate(T subject, String metadataKey) {
		return disambiguate(subject, metadataKey);
    }
}
