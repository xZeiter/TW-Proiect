Documentație – SmartGrade
Sistem inteligent de generare, scanare și evaluare automată a testelor tip grilă

1. Introducere
SmartGrade este o aplicație software destinată digitalizării procesului de evaluare academică prin teste tip grilă. 
Aplicația a fost concepută pentru a automatiza complet procesul de creare, distribuire, scanare și evaluare a testelor, 
reducând semnificativ timpul de corectare și eliminând erorile umane.

Din punct de vedere conceptual, SmartGrade este similară cu aplicații consacrate precum ZipGrade, în special în ceea ce 
privește gestionarea studenților și a claselor. Totuși, SmartGrade extinde acest model printr-o arhitectură modernă, 
control complet asupra layout-ului testelor și integrare directă cu un backend personalizat.
2. Scopul aplicației
Scopul principal al aplicației este crearea unei platforme flexibile și scalabile pentru evaluarea automată a testelor tip grilă.
Aceasta urmărește:
- automatizarea procesului de evaluare
- reducerea timpului de corectare
- eliminarea subiectivității
- suport pentru structuri de test personalizate
- centralizarea și stocarea sigură a rezultatelor
3. Aplicații similare și poziționare
SmartGrade se inspiră din aplicații precum ZipGrade, care permit scanarea testelor grilă și asocierea acestora cu studenți și clase.
Similaritățile includ:
- organizarea studenților pe clase
- asocierea testelor cu grupuri de studenți
- evaluare automată prin scanare

Diferențele majore constau în:
- control complet asupra structurii testului
- versionarea layout-urilor
- utilizarea QR code-urilor pentru identificare unică
- separarea backend-ului de serviciul de scanare
- posibilitatea extinderii cu funcționalități avansate
4. Funcționalități principale
4.1 Autentificare și securitate
Aplicația utilizează autentificare bazată pe JWT, asigurând acces securizat la resurse.

4.2 Management studenți și clase
Utilizatorul poate crea, edita și șterge studenți și clase, similar cu funcționalitatea oferită de ZipGrade.

4.3 Management teste
Fiecare test poate avea:
- număr variabil de întrebări
- număr variabil de răspunsuri
- ID extern configurabil
- layout-uri multiple

4.4 Generare foi de examen
Pentru fiecare student se generează un PDF unic, ce conține un QR code pentru identificare.

4.5 Scanare și evaluare automată
Foile sunt scanate, analizate și evaluate automat folosind procesare de imagine.
5. Arhitectura aplicației
Aplicația este structurată pe o arhitectură de tip client-server, compusă din:
- Frontend web pentru interacțiunea utilizatorului
- Backend Spring Boot pentru logica aplicației
- Serviciu Python FastAPI pentru scanare și procesare imagine

Această separare permite scalabilitate și mentenanță ușoară.
6. Modelul de date
Modelul de date este relațional și include entități precum:
- StudentService
- ClassService
- QuizEntity
- QuizLayoutEntity
- SheetService

Modelul permite versionarea layout-urilor și păstrarea istoricului complet al evaluărilor.
7. Fluxul aplicației
1. Crearea claselor și studenților
2. Crearea testului
3. Generarea foilor de examen
4. Scanarea foilor completate
5. Evaluarea automată
6. Salvarea și afișarea rezultatelor
8. Avantaje
- Similaritate cu ZipGrade, dar cu flexibilitate superioară
- Control complet asupra testelor
- Arhitectură modernă
- Scalabilitate ridicată
- Posibilități extinse de dezvoltare viitoare
9. Posibile extensii
- dashboard analitic
- rapoarte PDF
- export Excel
- integrare AI
- suport pentru teste multi-pagină
10. Concluzie
SmartGrade reprezintă o soluție modernă pentru evaluarea automată a testelor tip grilă.
Inspirată din aplicații precum ZipGrade, dar extinsă semnificativ din punct de vedere tehnic,
aplicația oferă un nivel ridicat de flexibilitate, securitate și performanță.
