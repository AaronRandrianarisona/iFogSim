# Prise de note et comtpe rendu !
par *Aaron RANDRIANARISONA* et *Valentin RICARDO*

## Préambule 
- Cloner le projet
- Remplacer les sources du projet par le `src` fournis sur Moodle

### Config
- Aller dans `src/org/fog/utils/Config.java`
- Changer la variable `MAX_SIMULATION_TIME` à 1000\*60\*60 (car en millisecondes)


1 Scénario = 
1. Générer UseCase avec du random (taille MIPS, RAM? TAILLE TUPLE, ETC...)
2. IMPORTER LE MÊME USECASE pour tout
3. Dump les valeurs aléatoires ! (à sauvegarder dans un fichier)

Bonus : Implémenter le waiting time

```java
AppEdge(<Capteur/Service source>, <service dest>, <donnée_générée TupleType>, CPULength, NWLength, period)

for sensor:Sensors{
    String ServiceHGW = getAssociateService(sensor);
    AddAppEdge(sensor.getName(), serviceHGW, "s"+s.getId(), random, random); // Lien Service producteur/consomateur par la donnée produite
}
for service:Services{
    String Sdest = getRandomService();
    AddAppEdge(service.getName, Sdest, "DS"+"service.getId(), random, random); //Sdest consomme la donnée DS
}

```

Le TupleMapping fait le lien entre donnée entrée et données sorties
Il prend => la donnée d'Entrée et produit

Le serviceDest = le service pris en param dans le TupleMapping