package cs455.overlay.node;

import cs455.overlay.routing.RegisterItem;
import cs455.overlay.routing.Route;
import cs455.overlay.routing.RoutingTable;
import cs455.overlay.transport.TCPConnection.TCPReceiver;
import cs455.overlay.transport.TCPConnection.TCPSender;
import cs455.overlay.util.CommandParser;
import cs455.overlay.wireformats.OverlayNodeSendsRegistration;
import cs455.overlay.wireformats.Protocol;
import cs455.overlay.wireformats.RegistryReportsRegistrationStatus;
import cs455.overlay.wireformats.RegistrySendsNodeManifest;
import dnl.utils.text.table.TextTable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

public class Registry extends Node{

    private ServerSocket server;
    private Hashtable<Integer, RegisterItem> registry;
    private RoutingTable[] manifests;
    private Hashtable<Integer, Socket> sockets;

    public static void main(String[] args) throws Exception {
        if (args.length != 1){
            System.out.println("USAGE: java cs455.overlay.node.Registry [Port Number]");
            System.exit(1);
        }
        Registry registry = new Registry(Integer.parseInt(args[0]));

    }

    public Registry(int port) throws Exception {
        sockets = new Hashtable<>();
        server = new ServerSocket(port);
        System.out.println("Registry running on " + InetAddress.getLocalHost().getHostAddress() + ":" + server.getLocalPort() + "...");
        registry = new Hashtable<>();
        new Thread(() -> cycle(), "Registry").start();
        new Thread(() -> new CommandParser().registryParser(this), "Command Parser").start();
    }

    private void cycle(){
        try {
            while(true){
                ServerWorker(server.accept());
                for (int i = 0 ; i < this.sockets.size() ; i++){
                    System.out.println(sockets.get(i).getPort() + " : " + sockets.get(i).isConnected());
                }
            }
        } catch (Exception e){
            System.out.println("[" + Thread.currentThread().getName() + "]: Error in server thread: " + e.getMessage());
            System.exit(1);
        }
    }

    private void ServerWorker(Socket socket){
        Thread server = new Thread(() -> {
            try{
                byte[] data = new TCPReceiver(socket).read();

                switch (data[0]) {
                    case Protocol.OVERLAY_NODE_SENDS_REGISTRATION:
                        OverlayNodeSendsRegistration ONSR = new OverlayNodeSendsRegistration();
                        ONSR.craft(data);

                        int id = -1;
                        String message = "";

                        // if ONSR.ipToString().equals(socket.getInetAddress())
                        try {
                            id = register(new RegisterItem(ONSR.getIp(), ONSR.getPort()));
                            message = "Registration request successful. There are currently (" + registry.size() + ") nodes constituting the overlay.";
                            socket.setKeepAlive(true);
                            sockets.put(id, socket);
                        } catch (Exception e) {
                            message = e.getMessage();
                        }

                        RegistryReportsRegistrationStatus RRRS = new RegistryReportsRegistrationStatus(id, message);
                        new Thread(() -> {
                            try {
                                new TCPSender(socket).sendData(RRRS.pack());
                            } catch (Exception e) {

                            }
                        }).start();
                }
            }catch (Exception e){
                ;
            }
        });
        server.start();
    }

    /**
     * @author: Josh Mau | 1/21/2018
     * Synchronized register so two nodes will not have the same ID if two
     * nodes request registration at the same time.
     * Must setID as current size first (since 0 based), then add the item
     * to the registry so the size will be incremented next time.
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
        this.manifests = new RoutingTable[registry.size()];

        for (int i = 0 ; i < registry.size() ; i++){

            RoutingTable temp = new RoutingTable();

            for (int j = 0 ; j < size ; j++){
                int pow = (int)Math.pow(2, j);
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
        try {
            for (int i = 0; i < this.sockets.size(); i++) {
                TCPSender send = new TCPSender(this.sockets.get(i));
                send.sendData(new RegistrySendsNodeManifest(this.manifests[i], this.getAllNodes()).pack());
            }
        }catch (Exception e){
            System.out.println("error " + e.getMessage());
        }
        printManifests();
    }

    public void sendManifest(int index) throws Exception {
        try {
            RegistrySendsNodeManifest RSNM = new RegistrySendsNodeManifest();
            byte[] data = RSNM.pack();
            Socket s = sockets.get(index);
            new Thread(() -> {
                try {
                    TCPSender send = new TCPSender(s);
                    send.sendData(data);
                } catch (Exception e) {
                    System.out.println("Error sending manifest: " + e.getMessage());
                }
            });
        } catch (Exception e){
            System.out.println("Error sending manifest: " + e.getMessage());
            throw new Exception("Error sending manifest: " + e.getMessage());
        }
    }

    public void printManifests(){
        int counter = 0;
        for (RoutingTable r : manifests){
            System.out.println("Node " + counter++);
            System.out.println(r.toString());
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
}
