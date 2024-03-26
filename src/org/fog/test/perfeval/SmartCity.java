package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
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
	public static final float leftLatencyDC = 1000 ;
	public static final float rightLatencyDC = 1000;
	public static final float leftLatencyRFOG = 100;
	public static final float rightLatencyRFOG = 100;
	
	/*
	 * Up latencies eg between DC and RFOG child
	 */
	

	
	/* 
	 * infrastructure pour le projet 
	 */
	public static int nb_HGW=100; //5 HGW per LFOG
	public static final int nb_LFOG = 20; //2 LFOG per RFOG
	public static final int nb_RFOG = 10; //2  RFOG per DC
	public static final int nb_DC = 5; //
	
	
	/*
	 * La p�riode de g�n�ration des donn�es de capteurs 
	 */
	public static int sensor_periodicite = 1000;

	
	
	public static void main(String[] args) {

		Log.printLine("Starting Smart_City...");

		try {
			Log.disable();
//			Log.enable();
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
				
			
			

						
			/*
			 * Cr�ation de l'entit� controller et application du placement des services d�fini dans moduleMapping
			 */
			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
			
			controller.submitApplication(application, 0, new ModulePlacementMapping(fogDevices, application, moduleMapping));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			
			
			/*
			 * Calcul des latences physiques entre les noeuds de Fog
			 */
			System.out.println("Latencies computation...");
			TopologicalGraph graph = computeTopologicalGraph(fogDevices);
			new DelayMatrix_Float(graph, false);
			
			
			//printDevices();
			
			//printAllToAllLatencies();
			
			//System.out.println();
			

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
	}

	/**
	 * Creates the fog devices in the physical topology of the simulation.
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices(int userId, String appId) {
				
		/*
		 * Cr�ation des datacenters, il faut donner le nom "DC" suivi par un num�ro du datacenter, exemple DC0 pour le premier data center
		 * l'id du DC commence par 3, c'est normal
		 * id DC0 = 3
		 * id DC1 = 4, etc.
		 */
		int dc_mips = 1000, dcRAM = 1000*1000;

		ArrayList<FogDevice> dcs = new ArrayList<>();//Liste des datacenters


		for (int i = 0; i < nb_DC ; i++) {
			FogDevice dc = createFogDevice("DC"+i, dc_mips, dcRAM, 10000, 10000, 1, 0.0, 107.339,  83.4333);
			dc.setParentId(-1);
			dcs.add(dc);
			fogDevices.add(dc);
		}
		
		/*
		 * Cr�ation des RPOP, il faut donner le nom "RPOP" suivi par son num�ro 
		 */
		int rpop_mips = 500, rpopRAM = 10*1000;

		ArrayList<FogDevice> rpops = new ArrayList<>();

		int quotaRFOG = nb_RFOG / nb_DC ;
		int dcnum = 0;

		for (int i = 0; i < nb_RFOG ; i++) {
			FogDevice rpop = createFogDevice("RPOP"+i, rpop_mips, rpopRAM, 10000, 10000, 1, 0.0, 107.339,  83.4333);
			
			if (i % quotaRFOG == 0) {
				dcnum += 1;
			}

			rpop.setParentId(dcs.get(dcnum - 1).getId());
			rpops.add(rpop);
			fogDevices.add(rpop);
		}

		/*
		 * Cr�ation des LPOP, il faut donner le nom "LPOP" suivi par son num�ro 
		 */
		int lpop_mips = 200, lpopRAM = 5 * 1000;

		ArrayList<FogDevice> lpops = new ArrayList<>();

		int quotaLFOG = nb_LFOG / nb_RFOG;

		int rfognum = 0;
		for (int j = 0; j < nb_LFOG; j++) {
			FogDevice lpop =  createFogDevice("LPOP"+j, lpop_mips, lpopRAM, 10000, 10000, 1, 0.0, 107.339, 83.4333);

			if (j % quotaLFOG == 0) {
				rfognum += 1;
			}

			lpop.setParentId(rpops.get(rfognum - 1).getId());
			lpops.add(lpop);
			fogDevices.add(lpop);
		}
		
		/*
		 * Cr�ation des Passerelles, il faut donner le nom "HGW" suivi par son num�ro 
		 */
		int hgw_mips = 100, hgwRAM = 1000;

		
		ArrayList<FogDevice> hgws = new ArrayList<>();

		int quotaHGW = nb_HGW / nb_LFOG;

		int lfgnum = 0;

		for (int i = 0; i < nb_HGW; i++) {
			FogDevice hgw =  createFogDevice("HGW"+i, hgw_mips, hgwRAM, 10000, 10000, 1, 0.0, 107.339, 83.4333); 

			if (i % quotaHGW == 0) {
				lfgnum += 1;
			}

			hgw.setParentId(lpops.get(lfgnum - 1).getId());
			hgws.add(hgw);
			fogDevices.add(hgw);
		}
		
		/*
		 * Cr�ation des capteurs, il faut donner le nom "s" suivi par son num�ro 
		 */
		Integer nbHGW_sensors = 10;

		int quotaSensors = nb_HGW / nb_LFOG;

		int hgwnum = 0;

		for (int i = 0; i < nbHGW_sensors * nb_HGW; i++) {
			Sensor sensor = new Sensor("s"+i, "D"+i, userId, appId, new DeterministicDistribution(sensor_periodicite));
			if (i % quotaSensors == 0) {
				hgwnum += 1;
			}
			sensor.setGatewayDeviceId(hgwnum - 1);
			sensors.add(sensor);
		}

	}

	
	/**
	 * Creates a vanilla fog device
	 * @param nodeName name of the device to be used in simulation
	 * @param mips MIPS
	 * @param ram RAM
	 * @param upBw uplink bandwidth
	 * @param downBw downlink bandwidth
	 * @param level hierarchy level of the device
	 * @param ratePerMips cost rate per MIPS used
	 * @param busyPower
	 * @param idlePower
	 * @return
	 */
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		System.out.println("Create FogDevice : "+nodeName);
		
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
				new FogLinearPowerModel(busyPower, idlePower)
			);

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
			fogdevice = new FogDevice(nodeName, characteristics,new AppModuleAllocationPolicy(hostList), storageList,
					right, left, getRightLatency(nodeName, right),getLeftLatency(nodeName, left), 10, upBw, downBw, 0,ratePerMips);
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
		
		} else if (nodeName.startsWith("RFOG")){
			fogId = Integer.valueOf(nodeName.substring(4));
			
			if (fogId > 0) {
				return fogId - 1 + 3 + nb_DC; // fogId le num�ro du RFOG ; - 1 pour avoir le noeud de gauche; +3 car fogdevice id commence par 3; +nb_DC pour aller aux RFOGs
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
		}
		else if ((nodeName.startsWith("RFOG"))) {
			fogId = Integer.valueOf(nodeName.substring(4));
			if ((nb_RFOG > 1) && (fogId < (nb_RFOG - 1))) {
				return fogId + 1 + 3 + nb_DC;
			} 
		}
		return -1;
	}

	
	/**
	 * Function to create the EEG Tractor Beam game application in the DDF model. 
	 * @param appId unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId){
		
		System.out.println("\nCreating Application");
		
		Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
		
		/*
		 * ajout des services (AppMdoule) - mips et ram demand�s
		 */
		Integer minRAM = 100, maxRAM = 1000;

		for (int i = 0; i < 1000; i++) {
			application.addAppModule("S" + i, minRAM + (int) (Math.random() * ((maxRAM - minRAM) + 1)));
		}

		

		/*
		 * ajout des d�pendance de donn�es entre les services
		 */		
				
			
		/*
		 * ajout des taux de production des donn�es (de sortie) en fonction du nombre des donn�es trait�es (d'entr�e) 
		 * 1 ==> 100%
		 * 0.5 ==> 50 %
		 * 0.1 ==> 10%
		 */
		

		
		/*
		 * Defining application loops to monitor the latency of. 
		 */
		
		
		return application;
	}
	
	public static TopologicalGraph computeTopologicalGraph(List<FogDevice> fogDevices){
		
		TopologicalGraph graph = new TopologicalGraph();
		
		TopologicalNode node =null;
		TopologicalLink link =null;
//		System.out.println("Graph construction...");
		
		for(FogDevice fogDevice : fogDevices){
			
			node =  new TopologicalNode(fogDevice.getId()-3, fogDevice.getName(),0,0);
			graph.addNode(node);
			
			/* ADD cheldren nodes */
			if(fogDevice.getChildrenIds() != null){
				Map<Integer, Double> childMap = fogDevice.getChildToLatencyMap();
				for(Integer key : childMap.keySet()){
					link = new TopologicalLink(fogDevice.getId()-3,(int) key-3, childMap.get(key).floatValue() , (float)30000);
					graph.addLink(link);
				}
			}
			
	
			/* ADD Right Link to Graph */
			if(fogDevice.getRightId()!=-1){
				link = new TopologicalLink(fogDevice.getId()-3,fogDevice.getRightId()-3, fogDevice.getRightLatency(),30000);
				graph.addLink(link);
			}
		}
		
		
		//System.out.println(graph.toString());
		
		return graph;

		
	}
	
	private static void printDevices() {
		System.out.println("\nFog devices : ");
		for (FogDevice fogdev : fogDevices) {
			 System.out.println(fogdev.getName()+"  idEntity = "+fogdev.getId()+" up= "+fogdev.getParentId()+" left ="+fogdev.getLeftId()+" leftLatency = "+fogdev.getLeftLatency()+" right ="+fogdev.getRightId()+" rightLatency="+fogdev.getRightLatency()+" children = "+fogdev.getChildrenIds()+" childrenLatencies ="+fogdev.getChildToLatencyMap()+" Storage = "+fogdev.getVmAllocationPolicy().getHostList().get(0).getStorage()+" |	");
		}

		// System.out.println("\nSensors : ");
		for (Sensor snr : sensors) {
			 System.out.println(snr.getName()+"  HGW_ID = "+snr.getGatewayDeviceId()+" TupleType = "+snr.getTupleType()+" Latency = "+snr.getLatency()+" |	");
		}
		// System.out.println("\nActuators : ");
		for (Actuator act : actuators) {
			 System.out.println(act.getName()+" GW_ID = "+act.getGatewayDeviceId()+" Act_Type= "+act.getActuatorType()+" Latency = "+act.getLatency()+" |	");
		}
		 System.out.println("\n");

	}
	
	private static void printAllToAllLatencies() {
		System.out.println("\nprint AllToAll Latencies");
		for (FogDevice src : fogDevices) {
			for (FogDevice dest : fogDevices) {
				System.out.println("Latency from "+src.getName()+" To "+dest.getName()+" = " +DelayMatrix_Float.getFastestLink(src.getId(), dest.getId()));
			}
			System.out.println();
		}
	}
}
