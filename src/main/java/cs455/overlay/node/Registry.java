package cs455.overlay.node;

import cs455.overlay.routing.RegisterItem;
import cs455.overlay.routing.Route;
import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.transport.TCPConnectionsCache;
import cs455.overlay.util.CommandParser;
import cs455.overlay.wireformats.*;

import dnl.utils.text.table.TextTable;
import java.util.Hashtable;
import java.util.Map;

public class Registry extends Node{

    private Hashtable<Integer, RegisterItem> registry;
    private RoutingTable[] manifests;
    private TCPConnectionsCache cache;
    private TCPServerThread tcpServer;
    private int port;

    public static void main(String[] args) throws Exception {
        if (args.length != 1){
            System.out.println("USAGE: java cs455.overlay.node.Registry [Port Number]");
            System.exit(1);
        }
        Registry registry = new Registry(Integer.parseInt(args[0]));
        registry.init();

    }

    public Registry(int port) throws Exception {
        this.cache = new TCPConnectionsCache();
        this.registry = new Hashtable<>();
        this.port = port;
    }

    public void init() throws Exception {
        new Thread(this.tcpServer = new TCPServerThread(port), "Registry").start();
        new Thread(() -> new CommandParser().registryParser(this), "Command Parser").start();
        new EventFactory(this);
    }

    public void onEvent(TCPConnection conn, Event e) throws Exception {
        switch (e.getType()){
            case Protocol.OVERLAY_NODE_SENDS_REGISTRATION:
                int id = -1;
                String message = "";
                OverlayNodeSendsRegistration ONSR = (OverlayNodeSendsRegistration)e;

                try {
                    id = register(new RegisterItem(ONSR.getIp(), ONSR.getPort()));
                    message = "Registration request successful. There are currently (" + registry.size() + ") nodes constituting the overlay.";
                    this.cache.addConnection(id, conn);
                } catch (Exception err) {
                    System.out.println(err);
                    message = err.getMessage();
                }

                RegistryReportsRegistrationStatus RRRS = new RegistryReportsRegistrationStatus(id, message);
                try {
                    conn.sendData(RRRS.pack());
                } catch (Exception err){
                    System.out.println(err);
                }
                break;
            case Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS:
                NodeReportsOverlaySetupStatus NROSS = (NodeReportsOverlaySetupStatus)e;
                if (NROSS.getStatusOrId() != -1) {
                    this.registry.get(NROSS.getStatusOrId()).setReady();
                    System.out.println("Node id: " + NROSS.getStatusOrId() + ": " + NROSS.getMessage() + ".. Node set to ready state.");
                }
                break;
            case Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED:
                int completeCount = 0;
                OverlayNodeReportsTaskFinished ONRTF = (OverlayNodeReportsTaskFinished)e;
                this.registry.get(ONRTF.getGuid()).setComplete();
                System.out.println("Task complete for node " + ONRTF.getGuid());
                for (Map.Entry<Integer, RegisterItem> entry : this.registry.entrySet()){
                    if (this.registry.get(entry.getKey()).isComplete()){
                        completeCount += 1;
                    }
                }
                if (completeCount == this.registry.size()){
                    gatherTaskData();
                }
        }

    }

    /**
     * @method: Synchronized register:
     *  two nodes will not have the same ID if two
     *  nodes request registration at the same time.
     *  Must setID as current size first (since 0 based), then add the item
     *  to the registry so the size will be incremented next time.
     * */
    private synchronized int register(RegisterItem ri) throws Exception {
        if (!registry.containsValue(ri)){
            if (registry.size() < 127) {
                ri.setId(registry.size());
                registry.put(registry.size(), ri);
            } else {
                throw new Exception("Registry is full.");
            }
        } else {
            throw new Exception("This node is already registered");
        }
        return ri.getId();
    }

    public void generateManifests(int size) throws Exception {
        if (this.registry.size() != 0) {
            this.manifests = new RoutingTable[registry.size()];

            for (int i = 0; i < registry.size(); i++) {

                RoutingTable temp = new RoutingTable();

                for (int j = 0; j < size; j++) {
                    int pow = (int) Math.pow(2, j);
                    int index = pow + i;

                    if (index < registry.size()) {
                        RegisterItem ri = registry.get(index);
                        temp.addRoute(new Route(ri.getIp(), ri.getPort(), ri.getId()));
                    } else {
                        index -= registry.size();
                        RegisterItem ri = registry.get(index);
                        temp.addRoute(new Route(ri.getIp(), ri.getPort(), ri.getId()));
                    }
                }
                manifests[i] = temp;
            }

            this.cache.doForAll((Integer id) -> {
                try {
                    RoutingTable r = this.manifests[id];
                    RegistrySendsNodeManifest RSNM = new RegistrySendsNodeManifest(r, this.getAllNodes());
                    this.cache.getConnectionById(id).sendData(RSNM.pack());
                } catch (Exception e) {
                    ;
                }
                return true;
            });
        } else {
            System.out.println("No nodes have been registered with the system.");
        }
    }

    public void printManifests(){
        if (this.manifests != null) {
            int counter = 0;
            synchronized (System.out) {
                for (RoutingTable r : manifests) {
                    System.out.println("Node " + counter++);
                    System.out.println(r.toString());
                }
            }
        } else {
            System.out.println("No overlay has been configured. Use command \"setup-overlay [manifest size]\" to configure an overlay.");
        }
    }

    public void listMessagingNodes(){
        String[][] data = new String[this.getSize()][3];
        for (int i = 0 ; i < registry.size() ; i++) {
            data[i][0] = registry.get(i).ipToString();
            data[i][1] = Integer.toString(registry.get(i).getPort());
            data[i][2] = Integer.toString(registry.get(i).getId());

        }
        System.out.println("There " + (this.getSize() == 1 ? "is 1 node registered with the registry." :
                "are " + this.getSize() + " nodes registered with the registry."));
        TextTable tt = new TextTable(new String[]{"Host", "Port", "Node ID"}, data);
        tt.printTable();
        System.out.println();
    }

    public Hashtable<Integer, RegisterItem> getRegistry() { return registry; }
    public int getSize() {
        return registry.size();
    }
    public int[] getAllNodes() {
        int[] nodes = new int[this.getSize()];
        int counter = 0;
        for (Integer key : this.registry.keySet()){
            nodes[counter++] = key;
        }
        return nodes;
    }

    public void initDataStream(int numDataPackets){
        this.cache.doForAll((Integer i) -> {
            try {
                this.cache.getConnectionById(i).sendData(new RegistryRequestsTaskInitiate(numDataPackets).pack());
            } catch (Exception e){
                ;
            }
            return true;
        });
    }

    private void gatherTaskData(){
        this.cache.doForAll((Integer i) -> {
            try {
                this.cache.getConnectionById(i).sendData(new RegistryRequestsTrafficSummary().pack());
            } catch (Exception e){
                System.out.println("[" + Thread.currentThread().getName() + "] Error requesting task summary: " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        });
    }
}
