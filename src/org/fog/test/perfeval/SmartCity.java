package org.fog.test.perfeval;

import java.time.Duration;
import java.time.Instant;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.DelayMatrix_Float;
import org.cloudbus.cloudsim.network.TopologicalGraph;
import org.cloudbus.cloudsim.network.TopologicalLink;
import org.cloudbus.cloudsim.network.TopologicalNode;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for case study 1 - EEG Beam Tractor Game
 * 
 * @author Harshit Gupta
 *
 */
public class SmartCity {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();

	/*
	 * Left & right latency for Datacenter pour le projet
	 */
	public static final float leftLatencyDC = 1000;
	public static final float rightLatencyDC = 1000;
	public static final float leftLatencyRFOG = 100;
	public static final float rightLatencyRFOG = 100;

	/*
	 * Up latencies eg between DC and RFOG child
	 */
	public static final float LatencyDCRFOG = 1000;
	public static final float LatencyLFOGRFOG = 100;
	public static final float LatencyLFOGPass = 50;
	public static final float LatencyCaptPass = 10;

	/*
	 * infrastructure pour le projet
	 */
	public static final int nb_DC = 5; //
	public static final int nb_RFOG_per_DC = 2; // 2 RFOG per DC
	public static final int nb_RFOG = nb_RFOG_per_DC * nb_DC; // 10 RFOG
	public static final int nb_LFOG_per_RFOG = 2; // 2 LFOG per RFOG
	public static final int nb_LFOG = nb_LFOG_per_RFOG * nb_RFOG; // 20 LFOG
	public static final int nb_HGW_per_LFOG = 1; // 1 HGW per LFOG
	public static final int nb_HGW = nb_HGW_per_LFOG * nb_LFOG; // 20 HGW

	public static final int nb_Sensor_per_HGW = 1; // 1 sensor per gateway
	public static final int nb_Sensor = nb_Sensor_per_HGW * nb_HGW; // 20 sensors

	public static final int nb_service = 200; // 200 services

	/*
	 * La p�riode de g�n�ration des donn�es de capteurs
	 */
	public static int sensor_periodicite = 1000;

	public static void main(String[] args) {

		Log.printLine("Starting Smart_City...");

		long start = System.currentTimeMillis();

		try {
			Log.disable();
			// Log.enable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "Smart_City"; // identifier of the application

			/*
			 * La creation de l'entite brocker
			 */
			FogBroker broker = new FogBroker("broker");

			/*
			 * Creation des noeuds de Fog et des capteurs
			 */
			createFogDevices(broker.getId(), appId);

			/*
			 * Creation de l'application
			 */
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());

			/*
			 * Creation du catalogue des emplacements des services
			 */
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping

			/*
			 * Placement des services dans les noeuds de Fog
			 */

			for (int i = 0; i < nb_HGW; i++) {
                moduleMapping.addModuleToDevice("S" + i, "HGW" + i);
            }

            // CLOUD Version
            //cloudPlacement(moduleMapping);

            // RANDOM Version
			//randomPlacement(moduleMapping,application);

            // Fog1 Version
            fog1Placement(moduleMapping,application);

            // Fog2 Version
            // fog2Placement(moduleMapping,application);
			
			

						
			/*
			 * Cr�ation de l'entit� controller et application du placement des services
			 * d�fini dans moduleMapping
			 */
			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
			System.out.println("Module mapping : " + moduleMapping.toString());
			controller.submitApplication(application, 0,
					new ModulePlacementMapping(fogDevices, application, moduleMapping));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			/*
			 * Calcul des latences physiques entre les noeuds de Fog
			 */
			System.out.println("Latencies computation...");
			TopologicalGraph graph = computeTopologicalGraph(fogDevices);
			new DelayMatrix_Float(graph, false);

			printDevices();

			printAllToAllLatencies();

			System.out.println();

			/*
			 * Lancement de la simualtion
			 */
			CloudSim.startSimulation();

			/*
			 * Arret de la simulation
			 */
			CloudSim.stopSimulation();

			Log.printLine("Smart_City finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		long end = System.currentTimeMillis();
		System.out.println("Elapsed Time in milli seconds: "+ (end-start));
	} // fin main

    /******** LISTE DES ALGORYTHMES ********/

    // N°1 : Version CLOUD
    public static void cloudPlacement(ModuleMapping moduleMapping){
        for (int i = nb_HGW; i < nb_service; i++) {
            // Ajout des service dans un des datacenters au hasard
            int dc = Math.round((float) Math.random() * (nb_DC - 1));
            moduleMapping.addModuleToDevice("S" + i, "DC" + dc);
        }
    }

    // N°2 : Version RANDOM
    public static void randomPlacement(ModuleMapping moduleMapping, Application application) { // Ajout des service dans un des fogdevices au hasard
        for (int i = nb_HGW; i < nb_service; i++) {                
            // On prend aléatoirement un appareil parmi les FogDevices
            // Tant que le device n'a pas les mips ni la ram nécéssaire pour le service, on en prend aléatoirement un autre
			int device = Math.round((float) Math.random() * (fogDevices.size() - 1));
			boolean isMipsSpaceLargeEnough = fogDevices.get(device).getHost().getAvailableMips() >= application.getModuleByName("S" + i).getMips();
			boolean isRamSpaceLargeEnough = fogDevices.get(device).getHost().getRamProvisioner().getAvailableRam() >= application.getModuleByName("S" + i).getRam();

			while (!isMipsSpaceLargeEnough || !isRamSpaceLargeEnough) {
				device = Math.round((float) Math.random() * (fogDevices.size() - 1));
				isMipsSpaceLargeEnough = fogDevices.get(device).getHost().getAvailableMips() >= application.getModuleByName("S" + i).getMips();
                isRamSpaceLargeEnough = fogDevices.get(device).getHost().getRamProvisioner().getAvailableRam() >= application.getModuleByName("S" + i).getRam();
			}	

            // Ajout du service sur le FogDevice
            moduleMapping.addModuleToDevice("S" + i, fogDevices.get(device).getName());
        }
    }

    // N°3 : Version FOG1
    public static void fog1Placement(ModuleMapping moduleMapping, Application application){
        // Stratégie de placement : 3 services par fogDevices (HGW -> LFogX -> RFogX -> DataCenterX)

        // Tri des FogDevices dans l'ordre croissant en fonction des MIPS disponibles
		ArrayList<FogDevice> sortedFogDevices = new ArrayList<>(fogDevices);

		// Tri sur les fogdevices pour exclure les HGW
		sortedFogDevices.sort(Comparator.comparing(fd -> ((FogDevice) fd).getHost().getTotalMips()));

		int j = 0;
		for (int i = nb_HGW; i < nb_service; i++) {
			boolean isMipsSpaceLargeEnough = sortedFogDevices.get(j).getHost().getAvailableMips() >= application.getModuleByName("S" + i).getMips();
			boolean isRamSpaceLargeEnough = fogDevices.get(j).getHost().getRamProvisioner().getAvailableRam() >= application.getModuleByName("S" + i).getRam();

			// Ajout du service sur le premier FogDevice qui possède les ressources nécessaires
			while (!isMipsSpaceLargeEnough || !isRamSpaceLargeEnough) {
				j += 1;
				isMipsSpaceLargeEnough = sortedFogDevices.get(j).getHost().getAvailableMips() >= application.getModuleByName("S" + i).getMips();
				isRamSpaceLargeEnough = fogDevices.get(j).getHost().getRamProvisioner().getAvailableRam() >= application.getModuleByName("S" + i).getRam();
			}
			moduleMapping.addModuleToDevice("S" + i, sortedFogDevices.get(j).getName());
		}
		
	}// Fin FOG1

	// N°4 : Version FOG2
	public static void fog2Placement(ModuleMapping moduleMapping, Application application){
		// Statégie de placement : Du plus grand au plus petit (RFogX -> LFogX -> HGW -> DataCenterX)


		/***** TRI DE LA LISTE DES FOG DEVICES *****/
		// On souhaite trier les FogDevices en fonction des MIPS disponibles, sauf les datacenters
		ArrayList<FogDevice> sortedFogDevices = new ArrayList<>(fogDevices);
		sortedFogDevices.removeIf(fd -> fd.getName().startsWith("DC")); // Exclure les datacenters
		sortedFogDevices.sort(Comparator.comparing(fd -> ((FogDevice) fd).getHost().getTotalMips()).reversed());

		// Ajout des datacenters à la fin de la liste triée
		ArrayList<FogDevice> datacenters = new ArrayList<>();
		for (FogDevice fd : fogDevices) {
			if (fd.getName().startsWith("DC")) {
				datacenters.add(fd);
			}
		}
		sortedFogDevices.addAll(datacenters);

		/*****  DÉPLOIEMENT DES SERVICES SUR LES FOGDEVICES TRIÉS ORDRE DESCENDANT *****/
		int j = 0;
		for (int i = 0; i < nb_service; i++) {
			// Assurez-vous que l'index est dans les limites de la liste des dispositifs
			// triés
			boolean isMipsSpaceLargeEnough = sortedFogDevices.get(j).getHost().getAvailableMips() >= application.getModuleByName("S" + i).getMips();
			boolean isRamSpaceLargeEnough = fogDevices.get(j).getHost().getRamProvisioner().getAvailableRam() >= application.getModuleByName("S" + i).getRam();

			// Ajout du service sur le premier FogDevice qui possède les ressources nécessaires
			while (!isMipsSpaceLargeEnough || !isRamSpaceLargeEnough) {
				j += 1;
				isMipsSpaceLargeEnough = sortedFogDevices.get(j).getHost().getAvailableMips() >= application.getModuleByName("S" + i).getMips();
				isRamSpaceLargeEnough = fogDevices.get(j).getHost().getRamProvisioner().getAvailableRam() >= application.getModuleByName("S" + i).getRam();
			}
			moduleMapping.addModuleToDevice("S" + i, sortedFogDevices.get(j).getName());
		}
	} // Fin FOG2

	/**
	 * Creates the fog devices in the physical topology of the simulation.
	 * 
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices(int userId, String appId) {

		/*
		 * Cr�ation des datacenters, il faut donner le nom "DC" suivi par un num�ro du
		 * datacenter, exemple DC0 pour le premier data center
		 * l'id du DC commence par 3, c'est normal
		 * id DC0 = 3
		 * id DC1 = 4, etc.
		 */
		int dc_mips = 1000, dcRAM = 1000 * 1000;

		ArrayList<FogDevice> dcs = new ArrayList<>();// Liste des datacenters

		for (int i = 0; i < nb_DC; i++) {
			FogDevice dc = createFogDevice("DC" + i, dc_mips, dcRAM, 10000, 10000, 1, 0.01, 107.339, 83.4333);
			dc.setParentId(-1);
			dcs.add(dc);
			fogDevices.add(dc);
		}

		/*
		 * Cr�ation des Rfog, il faut donner le nom "Rfog" suivi par son num�ro
		 */
		int rfog_mips = 500, rfogRAM = 10 * 1000;

		ArrayList<FogDevice> rfogs = new ArrayList<>();

		int quotaRFOG = nb_RFOG / nb_DC;
		int dcnum = 0;

		for (int i = 0; i < nb_RFOG; i++) {
			FogDevice rfog = createFogDevice("RFOG" + i, rfog_mips, rfogRAM, 10000, 10000, 1, 0.0, 107.339, 83.4333);

			if (i % quotaRFOG == 0) {
				dcnum += 1;
			}

			rfog.setParentId(dcs.get(dcnum - 1).getId());
			rfog.setUplinkLatency(LatencyDCRFOG);
			rfogs.add(rfog);
			fogDevices.add(rfog);
		}

		/*
		 * Cr�ation des Lfog, il faut donner le nom "Lfog" suivi par son num�ro
		 */
		int lfog_mips = 200, lfogRAM = 5 * 1000;

		ArrayList<FogDevice> lfogs = new ArrayList<>();

		int quotaLFOG = nb_LFOG / nb_RFOG;

		int rfognum = 0;
		for (int j = 0; j < nb_LFOG; j++) {
			FogDevice lfog = createFogDevice("LFOG" + j, lfog_mips, lfogRAM, 10000, 10000, 1, 0.0, 107.339, 83.4333);

			if (j % quotaLFOG == 0) {
				rfognum += 1;
			}

			lfog.setParentId(rfogs.get(rfognum - 1).getId());
			lfog.setUplinkLatency(LatencyLFOGRFOG);
			lfogs.add(lfog);
			fogDevices.add(lfog);
		}

		/*
		 * Cr�ation des Passerelles, il faut donner le nom "HGW" suivi par son num�ro
		 */
		int hgw_mips = 1500, hgwRAM = 1000;

		ArrayList<FogDevice> hgws = new ArrayList<>();

		int quotaHGW = nb_HGW / nb_LFOG;

		int lfognum = 0;

		for (int i = 0; i < nb_HGW; i++) {
			FogDevice hgw = createFogDevice("HGW" + i, hgw_mips, hgwRAM, 10000, 10000, 1, 0.0, 107.339, 83.4333);

			if (i % quotaHGW == 0) {
				lfognum += 1;
			}

			hgw.setParentId(lfogs.get(lfognum - 1).getId());
			hgw.setUplinkLatency(LatencyLFOGPass);
			hgws.add(hgw);
			fogDevices.add(hgw);
		}

		/*
		 * Cr�ation des capteurs, il faut donner le nom "s" suivi par son num�ro
		 */
		int quotaSensors = nb_Sensor_per_HGW;

		int hgwnum = 0;

		for (int i = 0; i < nb_Sensor; i++) {
			Sensor sensor = new Sensor("s" + i, "D" + i, userId, appId,
					new DeterministicDistribution(sensor_periodicite));
			if (i % quotaSensors == 0) {
				hgwnum += 1;
			}
			sensor.setGatewayDeviceId(hgwnum - 1);
			double latency = Float.valueOf(LatencyCaptPass).doubleValue();
			sensor.setLatency(latency);
			sensors.add(sensor);
		}

	}

	/**
	 * Creates a vanilla fog device
	 * 
	 * @param nodeName    name of the device to be used in simulation
	 * @param mips        MIPS
	 * @param ram         RAM
	 * @param upBw        uplink bandwidth
	 * @param downBw      downlink bandwidth
	 * @param level       hierarchy level of the device
	 * @param ratePerMips cost rate per MIPS used
	 * @param busyPower
	 * @param idlePower
	 * @return
	 */
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {

		System.out.println("Create FogDevice : " + nodeName);

		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
		// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		int right = getRight(nodeName);
		int left = getleft(nodeName);

		try {
			fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList,
					right, left, getRightLatency(nodeName, right), getLeftLatency(nodeName, left), 10, upBw, downBw, 0,
					ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		return fogdevice;
	}

	private static float getRightLatency(String nodeName, int right) {
		if ((nodeName.startsWith("DC")) && (right != -1))
			return rightLatencyDC;
		if ((nodeName.startsWith("RFOG")) && (right != -1))
			return rightLatencyRFOG;
		return -1;
	}

	private static float getLeftLatency(String nodeName, int left) {
		if ((nodeName.startsWith("DC")) && (left != -1))
			return leftLatencyDC;
		else if ((nodeName.startsWith("RFOG")) && (left != -1))
			return leftLatencyRFOG;
		return -1;
	}

	private static int getleft(String nodeName) {
		int fogId;
		if ((nodeName.startsWith("DC"))) {
			fogId = Integer.valueOf(nodeName.substring(2));
			if (fogId > 0) {
				return fogId - 1 + 3;
			}

		} else if (nodeName.startsWith("RFOG")) {
			fogId = Integer.valueOf(nodeName.substring(4));

			if (fogId > 0) {
				return fogId - 1 + 3 + nb_DC; // fogId le num�ro du RFOG ; - 1 pour avoir le noeud de gauche; +3 car
												// fogdevice id commence par 3; +nb_DC pour aller aux RFOGs
			}
		}

		return -1;
	}

	private static int getRight(String nodeName) {
		int fogId;
		if ((nodeName.startsWith("DC"))) {
			fogId = Integer.valueOf(nodeName.substring(2));
			if ((nb_DC > 1) && (fogId < (nb_DC - 1))) {
				return fogId + 1 + 3;
			}
		} else if ((nodeName.startsWith("RFOG"))) {
			fogId = Integer.valueOf(nodeName.substring(4));
			if ((nb_RFOG > 1) && (fogId < (nb_RFOG - 1))) {
				return fogId + 1 + 3 + nb_DC;
			}
		}
		return -1;
	}

	/**
	 * Function to create the EEG Tractor Beam game application in the DDF model.
	 * 
	 * @param appId  unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({ "serial" })
	private static Application createApplication(String appId, int userId) {

		System.out.println("\nCreating Application");

		Application application = Application.createApplication(appId, userId); // creates an empty application model
																				// (empty directed graph)

		/*
		 * ajout des services (AppMdoule) - mips et ram demand�s
		 */
		Integer minRAM = 100, maxRAM = 1000,minMips = 100, maxMips = 1000;

		for (int i = 0; i < nb_service; i++) {
			int randram = minRAM + (int) (Math.random() * ((maxRAM - minRAM) + 1));
			int randmips = minMips + (int) (Math.random() * ((maxMips - minMips) + 1));;
			application.addAppModule("S" + i,randmips,randram);
		}

		/*
		 * ajout des dépendances de données entre les services : les AppEdge
		 * un AppEdge fait le lien entre
		 * un capteur et un service
		 * ou
		 * un service et un capteur
		 * 
		 */

		// les dépendances de données entre les capteurs et les services placés dans les
		// HGW associés
		// capteurs de 0-9 avec le service 0 qui est placé dans le HGW0
		// capteur de 10-19 avec le service 1 qui est placé dans le HGW1x
		// vous pouvez nommer les données produites par un capteur par "DC"+i avec i est
		// l'id du capteur

		int hgwnum = 0;

		for (int i = 0; i < nb_Sensor; i++) {
			Sensor sensor = sensors.get(i);

			if (i % nb_Sensor_per_HGW == 0) {
				hgwnum += 1;
			}

			int nb_mi = (int) (Math.random() * 90 + 10); // entre 10 et 100
			int nb_ram = (int) (Math.random() * 900 + 100); // entre 100 et 1000

			application.addAppEdge(sensor.getName(), "S" + hgwnum, nb_mi, nb_ram, "D" + i, Tuple.UP, AppEdge.SENSOR);
		}

		// les dépendances de données entre les services et les services placés dans les
		// HGW associé
		// pour chaque service dans l'application, il faut choisir un service
		// consommateur aléatoirement parmi l'ensemble des services de l'application
		// vous pouvez nommer les données produites par un service par "DS"+i avec i est
		// le numéro du service

		for (int i = 0; i < nb_service; i++) {
			int serviceDestNumber = (int) (Math.random() * application.getModules().size() - 1);
			int nb_mi = (int) (Math.random() * 90 + 10); // entre 10 et 100
			int nb_ram = (int) (Math.random() * 900 + 100); // entre 100 et 1000
			application.addAppEdge("S" + i, "S" + serviceDestNumber, nb_mi, nb_ram, "DS" + i, Tuple.UP, AppEdge.MODULE);
		}

		
		for (int i = 0; i < nb_service; i++) {
			AppModule service = application.getModules().get(i);

			ArrayList<String> consumedData = getConsummedDataByService(service.getName(),application.getEdges());
			
			for(String d : consumedData) {
				application.addTupleMapping(service.getName(),d,"DS"+i, new FractionalSelectivity(0.1));
			}
		}

		/*
		 * Defining application loops to monitor the latency of.
		 */

		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				for (int i = 0; i < nb_Sensor; i++) {
					add("s" + i);
					add("D"+i);
				}
				for (int i = 0; i < nb_service; i++) {
					add("S" + i);
					add("DS" + i);
				}
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
			}
		};
		application.setLoops(loops);

		return application;
	}

	public static TopologicalGraph computeTopologicalGraph(List<FogDevice> fogDevices) {

		TopologicalGraph graph = new TopologicalGraph();

		TopologicalNode node = null;
		TopologicalLink link = null;
		// System.out.println("Graph construction...");

		for (FogDevice fogDevice : fogDevices) {

			node = new TopologicalNode(fogDevice.getId() - 3, fogDevice.getName(), 0, 0);
			graph.addNode(node);

			/* ADD cheldren nodes */
			if (fogDevice.getChildrenIds() != null) {
				Map<Integer, Double> childMap = fogDevice.getChildToLatencyMap();
				for (Integer key : childMap.keySet()) {
					link = new TopologicalLink(fogDevice.getId() - 3, (int) key - 3, childMap.get(key).floatValue(),
							(float) 30000);
					graph.addLink(link);
				}
			}

			/* ADD Right Link to Graph */
			if (fogDevice.getRightId() != -1) {
				link = new TopologicalLink(fogDevice.getId() - 3, fogDevice.getRightId() - 3,
						fogDevice.getRightLatency(), 30000);
				graph.addLink(link);
			}
		}

		// System.out.println(graph.toString());

		return graph;

	}

	private static void printDevices() {
		System.out.println("\nFog devices : ");
		for (FogDevice fogdev : fogDevices) {
			System.out.println(fogdev.getName() + "  idEntity = " + fogdev.getId() + " up= " + fogdev.getParentId()
					+ " left =" + fogdev.getLeftId() + " leftLatency = " + fogdev.getLeftLatency() + " right ="
					+ fogdev.getRightId() + " rightLatency=" + fogdev.getRightLatency() + " children = "
					+ fogdev.getChildrenIds() + " childrenLatencies =" + fogdev.getChildToLatencyMap() + " Storage = "
					+ fogdev.getVmAllocationPolicy().getHostList().get(0).getStorage() + " |	");
		}

		// System.out.println("\nSensors : ");
		for (Sensor snr : sensors) {
			System.out.println(snr.getName() + "  HGW_ID = " + snr.getGatewayDeviceId() + " TupleType = "
					+ snr.getTupleType() + " Latency = " + snr.getLatency() + " |	");
		}
		// System.out.println("\nActuators : ");
		for (Actuator act : actuators) {
			System.out.println(act.getName() + " GW_ID = " + act.getGatewayDeviceId() + " Act_Type= "
					+ act.getActuatorType() + " Latency = " + act.getLatency() + " |	");
		}
		System.out.println("\n");

	}

	private static void printAllToAllLatencies() {
		System.out.println("\nprint AllToAll Latencies");
		for (FogDevice src : fogDevices) {
			for (FogDevice dest : fogDevices) {
				System.out.println("Latency from " + src.getName() + " To " + dest.getName() + " = "
						+ DelayMatrix_Float.getFastestLink(src.getId(), dest.getId()));
			}
			System.out.println();
		}
	}

	private static ArrayList<String> getConsummedDataByService(String serviceName, List<AppEdge> edges) {
		ArrayList<String> consumedData = new ArrayList<>();
		for (AppEdge appEdge : edges) {
			if (appEdge.getSource().equals(serviceName)) {
System.out.println("Test consumed data :" + appEdge.getDestination());
				consumedData.add("D" + appEdge.getDestination());
			}
		}

		return consumedData;
	}
}