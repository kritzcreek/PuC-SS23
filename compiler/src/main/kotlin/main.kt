import kotlinx.collections.immutable.persistentMapOf

fun main() {
    val myMap = persistentMapOf("x" to 1, "y" to 2);
    val myNewMap = myMap.put("z", 3);
    println("Hello PuC 23 ${myMap}, $myNewMap")
}