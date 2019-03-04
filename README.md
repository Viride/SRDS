# SRDS
SRDS jest projektem przygotowanym na zajęcia wykorzystującym Apache Cassandra.

Opis działania projektu:
Projekt miał za zadanie odzwierciedlać rezerwowanie biletów na określony seans. Pojemność sali była reprezentowana przez ilość rzędów * liczba miejsc w rzędzie. 
Rezerwując klient wstawiał do przygotowanej w Cassandrze tabeli wpis z nazwą sali, swoim Id, ilością miejsc które chciał zarezerwować i timestamp wpisu. 
Następnie klient pobierał wpisy, które już w systemie były i na podstawie ich timestampów określał czy udało mu się zarezerwować pożądaną liczbę miejsc czy nie. Udawało mu się, gdy w którymkolwiek z rzędów było tyle lub więcej wolnych miejsc co chciał np. w rzędzie gdzie jest 10 miejsc, 5 już zajętych, żądanie z 4 się zmieści, a z 6 już nie. Algorytm przydzielania zapełniał salę od pierwszego rzędu do ostatniego i w momencie gdy w danym rzędzie nie było już miejsca, próbował znaleźć miejsce w kolejnym, aż nie miał możliwości przydzielenia.
Kontynuując klient był usypiany na ustalony okres czasu (pierwotnie 1s).
I ponownie pobierał listę wpisów, aby finalnie ustalić czy zdobył miejsce czy nie.

Podwójne pobieranie wpisów po uśpieniu ma na celu upewnienie się, że system zdążył zreplikować dane pomiędzy węzłami i ma taki sam stan.
Potencjalnie mogłoby się okazać, że podczas pierwszego pobrania część wpisów dokonanych na innym węźle nie została jeszcze przesłana. Co mogło spowodować, że dany klient pierwotnie myślał, że się dostał a po ustaleniu zgodnego stanu wyszłoby, że jednak nie.

Projekt bazowy, przerobiony do bierzącego stanu pochodzi z: http://www.cs.put.poznan.pl/tkobus/_downloads/CassandraDemo.tar.gz
