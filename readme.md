
## To run the project

__use__

```bash
./gradlew run
```

## Commands

#### *Format*

```bash
command <arg1> <arg2> ...
```

#### Commands list

-  Add client (добавить пользователя)
```bash
add_client <client_id: Int> <user_currency: String>
```

-  Deposit (пополнить счет)
```bash
deposit <client_id: Int> <amount: Double>
```

-  Withdraw (снять средства)
```bash
withdraw <client_id: Int> <amount: Double>
```

-  Exchange (обменять)
```bash
exchange <client_id: Int> <from_currency: String> <to_currency: String> <amount: Double>
```

- Transfer (перевод)
```bash
transfer <sender_id: Int> <receiver_id: Int> <amount: Double>
```