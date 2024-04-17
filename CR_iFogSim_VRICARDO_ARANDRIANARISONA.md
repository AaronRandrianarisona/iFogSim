# Comtpe Rendu Projet iFogSim <!-- omit in toc -->
par *Aaron RANDRIANARISONA* et *Valentin RICARDO*

## Table des matières <!-- omit in toc -->
- [1. Sujet du projet](#1-sujet-du-projet)
- [2. Configuration](#2-configuration)
- [3. Algorithmes de placement](#3-algorithmes-de-placement)
  - [3.1. Cloud](#31-cloud)
  - [3.2. Random](#32-random)
  - [3.3. Fog1](#33-fog1)
  - [3.4. Fog2](#34-fog2)
- [4. Comparaison des algorithmes](#4-comparaison-des-algorithmes)
- [5. Problèmes rencontrés](#5-problèmes-rencontrés)


## 1. Sujet du projet

Le sujet de projet porte sur l'écriture d'un scénario de "ville intelligente" (SmartCity) qui vise à améliorer les services et le bien-être des citoyens urbains en utilisant des outils intelligents pour gérer divers aspects (ex: le transport, la domotique, la santé, la distribution de l'énergie et de l'eau, etc...). L'objectif principal est de **déployer** ce scénario dans une **infrastructure de type Fog**, Cloud, comprenant des capteurs, des nœuds de Fog (Passerelles, LFOG, RFOG) et des centres de données. Les nœuds de Fog sont organisés de manière hiérarchique pour assurer la gestion efficace des services de la ville intelligente.

Chaque donnée produites dans le système est consommé par 1 unique service choisi aléatoirement.

## 2. Configuration
Dans notre cas d'utilisation, on a augmenté le temps de simulation pour le bien du projet. Si vous souhaitez le modifier, suivre la procédure :
- Aller dans `src/org/fog/utils/Config.java`
- Changer la variable `MAX_SIMULATION_TIME` à 1000\*60\*60 (car en millisecondes)

## 3. Algorithmes de placement
### 3.1. Cloud
Toutes les instances de services sont déployées dans les centres de données du Cloud

### 3.2. Random
Les instances de services sont déployées dans des nœuds de Fog (et centre de
données) choisis aléatoirement.

### 3.3. Fog1
Cet algorithme à une particularité de placement plutôt spéciale. Les instances de services et les nœuds de Fog doivent être triés en ordre ascendant selon les MI et MIPS disponibles.
Il doit y avoir 3 services par fogDevices (HGW -> LFogX -> RFogX -> DataCenterX), avec X un numéro aléatoire de fogDevice


### 3.4. Fog2
De même que pour l'algo de placement Fog1, les instances de services et les nœuds de Fog doivent être triés, mais cette fois en ordre descendant.
:warning: Attention à placer les DataCenter à la fin, sinon on retrouverait une configuration similaire à Cloud où tous les services sont déployés dans les DataCenter, ce qu'on ne veut pas. 

Cela nous donne un ordre comme ceci : (RFogX -> LFogX -> HGW -> DataCenterX)

## 4. Comparaison des algorithmes

<!-- TODO : Insérer un graphe comparatif -->

## 5. Problèmes rencontrés
Lors du développement du projet, nous et les autres groupes avons rencontrés pas mal de problèmes :

- **Difficile d'exécuter le projet**. Sans une bonne machine, il est impossible d'espérer reprendre ce projet
- **Difficulté du projet**. Pas évident de plonger dans le code et d'imaginer toute la suite des évènements sans un bon cahier de charges.