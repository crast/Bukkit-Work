
echo "About to run memory tests. Use a process viewer to see the memory use for these."

echo "FixedMetadataValue"
java -cp target/test-classes/:target/bukkit-1.4.7-R1.1-SNAPSHOT.jar org.bukkit.metadata.StaticFixedMemoryTest fixed
echo "===== DONE"

echo "StaticMetadataValue"
java -cp target/test-classes/:target/bukkit-1.4.7-R1.1-SNAPSHOT.jar org.bukkit.metadata.StaticFixedMemoryTest static
echo "===== DONE"

echo "FixedMetadataValueEx"
java -cp target/test-classes/:target/bukkit-1.4.7-R1.1-SNAPSHOT.jar org.bukkit.metadata.StaticFixedMemoryTest fixedex
echo "===== DONE"


echo "Going to run speed tests."
java -cp target/test-classes/:target/bukkit-1.4.7-R1.1-SNAPSHOT.jar org.bukkit.metadata.StaticFixedSpeedTest
