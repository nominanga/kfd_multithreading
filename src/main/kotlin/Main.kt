import java.util.concurrent.*
import kotlin.random.Random

data class Client(val id: Int, var balance: Double, var currency: String)

sealed class Transaction
data class Deposit(val clientId: Int, val amount: Double) : Transaction()
data class Withdrawal(val clientId: Int, val amount: Double, val forced: Boolean) : Transaction()
data class CurrencyExchange(val clientId: Int, val fromCurrency: String, val toCurrency: String, val amount: Double) : Transaction()
data class Transfer(val senderId: Int, val receiverId: Int, val amount: Double) : Transaction()

class Cashier(val bank: Bank) : Thread() {
    override fun run() {
        while (true) {
            val transaction = bank.transactionQueue.take()
            processTransaction(transaction)
        }
    }

    private fun deposit(clientId: Int, amount: Double) {
        val client = bank.clients[clientId]
        client?.let {
            it.balance += amount
            bank.notifyObservers("Added $amount ${it.currency} to user($clientId) balance")
        }
    }

    private fun withdraw(clientId: Int, amount: Double, forced: Boolean) {
        val client = bank.clients[clientId]
        client?.let {
            if (amount > it.balance) {
                bank.notifyObservers("Not enough funds to withdraw at user($clientId) balance, current balance is: ${it.balance}")
                return;
            }
            it.balance -= amount
            var message = "Withdraw $amount ${it.currency} from user($clientId)"

            if (forced) {
                message += ", which were extra after the currency exchange"
            }
            bank.notifyObservers(message)
        }
    }

    private fun currencyExchange(clientId: Int, fromCurrency: String, toCurrency: String, amount: Double) {
        val client = bank.clients[clientId]
        client?.let {
            val rateFrom = bank.getRateByCurrency(fromCurrency)
            val rateTo = bank.getRateByCurrency(toCurrency)

            if (rateTo < 0 || rateFrom < 0) {
                bank.notifyObservers("Wrong currency names")
                return;
            }

            val finalRate = rateFrom / rateTo
            val finalAmount = amount * finalRate

            it.balance -= amount
            bank.addTransaction(Withdrawal(clientId, it.balance, true))
            it.balance += finalAmount
            it.currency = toCurrency
            bank.notifyObservers("Exchanged $amount $fromCurrency to $finalAmount $toCurrency with rate: $finalRate")
        }
    }

    private fun transfer(senderId: Int, receiverId: Int, amount: Double) {
        val sender = bank.clients[senderId]
        val receiver = bank.clients[receiverId]

        if (sender != null && receiver != null && sender.balance >= amount && sender.currency == receiver.currency) {
            sender.balance -= amount
            receiver.balance += amount
            bank.notifyObservers("Sent $amount ${sender.currency} from user($senderId) to user($receiverId)")
        }
        else {
            bank.notifyObservers("Unable to send due to some reasons")
        }
    }

    private fun processTransaction(transaction: Transaction) {
        when (transaction) {
            is Deposit -> deposit(transaction.clientId, transaction.amount)
            is Withdrawal -> withdraw(transaction.clientId, transaction.amount, false)
            is CurrencyExchange -> currencyExchange(transaction.clientId, transaction.fromCurrency, transaction.toCurrency, transaction.amount)
            is Transfer -> transfer(transaction.senderId, transaction.receiverId, transaction.amount)
        }
    }
}

interface Observer {
    fun update(message: String)
}

class Logger : Observer {
    override fun update(message: String) {
        println("Log: $message")
    }
}

class Bank {
    private val random = Random.Default
    val clients = ConcurrentHashMap<Int, Client>()
    val cashiers = mutableListOf<Cashier>()
    private val exchangeRates = ConcurrentHashMap<String, Double>()
    val transactionQueue = LinkedBlockingQueue<Transaction>()
    private val observers = mutableListOf<Observer>()

    private fun getRandomExchangeRate() {
        exchangeRates["USD"] = random.nextDouble(90.0, 110.0)
        exchangeRates["EUR"] = random.nextDouble(105.0, 130.0)
        exchangeRates["GBP"] = random.nextDouble(150.0, 160.0)
    }

    fun getRateByCurrency(currency: String): Double {
        return exchangeRates.getOrDefault(currency, -1.0)
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun notifyObservers(message: String) {
        observers.forEach {
            it.update(message)
        }
    }

    fun getTasksAmountLeft(): Int {
        return transactionQueue.size
    }

    init {
        updateExchangeRates()
    }

    fun updateExchangeRates() {
        val executor = ScheduledThreadPoolExecutor(1)
        executor.scheduleAtFixedRate({
            getRandomExchangeRate()
        }, 0, 1, TimeUnit.HOURS)
    }

    fun addTransaction(transaction: Transaction) {
        transactionQueue.add(transaction)
    }

    fun addClient(clientId: Int, currency: String) {
        if (getRateByCurrency(currency) < 0) {
            notifyObservers("No such currency")
            return;
        }
        if (clients.containsKey(clientId)) {
            notifyObservers("Duplicate client $clientId")
            return;
        }
        clients[clientId] = Client(clientId, 0.0, currency)
    }


}



fun main() {
    val bank = Bank()
    val logger = Logger()

    bank.addObserver(logger)

    var cashier = Cashier(bank)
    bank.cashiers.add(cashier)
    cashier.start()


    while (true) {
        while (bank.getTasksAmountLeft() / 3 > bank.cashiers.size) {
            cashier = Cashier(bank)
            bank.cashiers.add(cashier)
            cashier.start()
        }


        try {
            val enter = readln()
            val input = enter.trim().split(" ")
            val command = input[0].lowercase()
            when (command) {
                "add_client" -> {
                    val id = input[1].toInt()
                    val userCurrency = input[2]
                    bank.addClient(id, userCurrency)
                }
                "deposit" -> {
                    val clientId = input[1].toInt()
                    val amount = input[2].toDouble()
                    bank.addTransaction(Deposit(clientId, amount))
                }

                "withdraw" -> {
                    val clientId = input[1].toInt()
                    val amount = input[2].toDouble()
                    bank.addTransaction(Withdrawal(clientId, amount, false))
                }

                "transfer" -> {
                    val senderId = input[1].toInt()
                    val receiverId = input[2].toInt()
                    val amount = input[3].toDouble()
                    bank.addTransaction(Transfer(senderId, receiverId, amount))
                }

                "exchange" -> {
                    val clientId = input[1].toInt()
                    val fromCurrency = input[2]
                    val toCurrency = input[3]
                    val amount = input[4].toDouble()
                    bank.addTransaction(CurrencyExchange(clientId, fromCurrency, toCurrency, amount))
                }

                "clear" -> {
                    repeat(50) { println() }
                }

                else -> println("Unknown command.")
            }
        } catch (e: Exception) {
            println("Wrong command format")
        }
    }


}