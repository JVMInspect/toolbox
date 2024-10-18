import java.io.File
import kotlin.math.cos
import kotlin.math.min

const val people = 1
const val case = 11.21
const val rounds = 100
const val columns = 1

fun main() {
    val opening = if (people > 1) case else case * columns

    val lines = File("winnings.txt")
        .readLines()

    val winnings = lines
        .mapIndexed { idx, line -> if (idx > rounds) null else line }
        .mapNotNull { it?.toFloat() }

    val sum = winnings.sum()
    val count = min(winnings.count(), rounds)

    val cost = count * opening
    val profit = sum - cost

    println("People: $people")
    println("Rounds: $count")
    println("Winnings: $$sum")
    println("Cost (total): $$cost")
    println("Cost (per case): $$opening")
    println("Cost (one round): $$opening")
    println("Profit (self): $${(sum / people) - cost}")
    println("Profit (overall): $$profit")
}