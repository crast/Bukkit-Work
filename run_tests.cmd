echo "About to run speed tests."

echo "Running... (10000/30)"
java -cp target/test-classes/:target/bukkit-1.5.1-R0.1-SNAPSHOT.jar org.bukkit.metadata.DisambiguateTest 10000 30
echo "===== DONE"
