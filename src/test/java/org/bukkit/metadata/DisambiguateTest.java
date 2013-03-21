package org.bukkit.metadata;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.TestPlugin;

import java.util.ArrayList;

class DisambiguateTest {
    private Plugin plugin = new TestPlugin("x");

	ArrayList<String> names = new ArrayList<String>();
	ArrayList<String> subjects = new ArrayList<String>();

	public static void main(String[] args) {
		new DisambiguateTest().run(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}

	public void run(int num_names, int num_subjects) {
		for (int i = 0; i < num_names; i++) {
			names.add("player" + i);
		}
		for (int i = 0; i < num_subjects; i++) {
			subjects.add("subject" + i);
		}

		// Run many times
		int n = 1;
		long elapsed = 0;
		while (elapsed < 10000) {
			MetadataStore<String> uncached = new UncachedStore();
			MetadataStore<String> cached = new CachedStore();

			// Put some data
			fill(uncached);
			fill(cached);
			System.out.println("#########");
			float hit_rate = ((float) (n - 1)) / ((float) n);
			System.out.println("Presumed hit rate: " + (hit_rate * 100.0) + "%");
			elapsed = runTest(uncached, n, "Uncached");
			elapsed += runTest(cached, n, "Cached");
			n *= 2;
		}
	}

	public void fill(MetadataStore<String> store) {
		int nsize = names.size() / 3;
		int ssize = subjects.size() / 3;
		for (int i = 0; i < nsize; i++) {
			for (int j = 0; j < ssize; j++) {
				store.setMetadata(names.get(i), subjects.get(j), new FixedMetadataValue(plugin, i + j));
			}
		}
	}

	public long runTest(MetadataStore<String> store, int loops, String description) {
		System.out.println(description + ": " + loops + " loops");
		long begin = System.currentTimeMillis();
		int n = 0;
		for (int loop = 0; loop < loops; loop++) {
			for (String name : names) {
				for (String subject : subjects) {
					n += (store.hasMetadata(name, subject)? 1 : 0);
				}
			}
		}
		long elapsed = System.currentTimeMillis() - begin;
		System.out.println("  -> elapsed: " + elapsed + "ms, N: " + n);
		return elapsed;
	}

	private class UncachedStore extends UncachedMetadataStoreBase<String> implements MetadataStore<String> {
		@Override
        protected String disambiguate(String subject, String metadataKey) {
            return subject + ":" + metadataKey;
        }
	}

	private class CachedStore extends MetadataStoreBase<String> implements MetadataStore<String> {
        @Override
        protected String disambiguate(String subject, String metadataKey) {
            return subject + ":" + metadataKey;
        }
    }
}
