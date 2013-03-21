package org.bukkit.metadata;


import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.TestPlugin;


class StaticFixedMemoryTest {
    private static Plugin fooPlugin = new TestPlugin("foo");
    private static final int NUM_OBJECTS = 1000000;

	public static void main(String[] args) {
	    String option = (args.length >= 1)? args[0] : "fixed";
	    MetadataValue[] values = new MetadataValue[NUM_OBJECTS];
	    for (int i = 0; i < NUM_OBJECTS; i++) {
	        if (option.equals("fixed")) {
	            values[i] = new FixedMetadataValue(fooPlugin, new Integer(42));
	        } else if (option.equals("static")) {
	            values[i] = new StaticMetadataValue(fooPlugin, new Integer(42));
	        } else if (option.equals("fixedex")) {
	        	values[i] = new FixedMetadataValueEx(fooPlugin, new Integer(42));
	        }
	    }
	    try {
    	    System.out.println("Objects made. About to sleep for 10 seconds.. (check memory now!)");
    	    Thread.sleep(10000);
    	    System.out.println("Now running value() on all objects.");
    	    for (MetadataValue v : values) {
    	        v.asInt();
    	    }
    	    System.out.println("About to sleep for 10 seconds again (check memory!)");
    	    Thread.sleep(10000);
	    } catch (InterruptedException e) {
	    }
	}
}
