package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RestauranteAgent extends Agent {

    private Map<String, OrderData> orders = new HashMap<>();
    private String restaurantName;
    private int basePrice = 15;
    private int avgPrepTime = 20;
    private jade.core.AID gestorAgent;

    private class OrderData {
        public String item;
        public String status;
        public long orderTime;

        OrderData(String item) {
            this.item = item;
            this.status = "RECIBIDO";
            this.orderTime = System.currentTimeMillis();
        }
    }

    protected void setup() {
        restaurantName = getLocalName();
        System.out.println("\n🍽️  " + restaurantName + " - Restaurante abierto y listo para recibir órdenes!");

        registrarServicio();
        buscarGestor();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();

                if (msg != null) {
                    String conversationId = msg.getConversationId();

                    if (msg.getPerformative() == ACLMessage.CFP) {
                        // Solicitud de presupuesto
                        handleQuoteRequest(msg, conversationId);

                    } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        // Orden aceptada, empezar a preparar
                        handleOrderAccepted(msg, conversationId);

                    } else if (msg.getPerformative() == ACLMessage.CANCEL) {
                        // Cancelación de orden
                        handleOrderCancellation(msg, conversationId);
                    }
                } else {
                    block();
                }
            }
        });

        // Simular preparación de órdenes
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                java.util.Iterator<String> it = orders.keySet().iterator();

                while (it.hasNext()) {
                    String orderId = it.next();
                    OrderData order = orders.get(orderId);

                    if (order.status.equals("PREPARANDO")) {
                        long elapsedTime = System.currentTimeMillis() - order.orderTime;
                        long prepTimeMs = avgPrepTime * 1000;

                        if (elapsedTime >= prepTimeMs) {
                            order.status = "LISTO";

                            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                            inform.setConversationId(orderId);
                            inform.setContent(order.item + " listo para entregar");
                            // Enviar al gestor
                            inform.addReceiver(gestorAgent);
                            send(inform);

                            System.out.println("🔔 " + restaurantName + " - " + order.item + " está LISTO!");
                        }
                    }
                }

                block(1000); // Revisa cada segundo
            }
        });
    }

    private void handleQuoteRequest(ACLMessage msg, String conversationId) {
        String item = msg.getContent();

        System.out.println("📋 " + restaurantName + " - Solicitud de presupuesto para: " + item);

        // Simular precios y tiempos realistas
        Random r = new Random();
        int price = basePrice + r.nextInt(10);
        int prepTime = avgPrepTime + r.nextInt(15);

        // Guardar orden en registro
        orders.put(conversationId, new OrderData(item));

        // Enviar propuesta
        ACLMessage proposal = msg.createReply();
        proposal.setPerformative(ACLMessage.PROPOSE);
        proposal.setContent("Precio: $" + price + " | Tiempo: " + prepTime + " min");
        send(proposal);

        System.out.println("💰 " + restaurantName + " - Propuesta enviada: $" + price + " en " + prepTime + " minutos");
    }

    private void handleOrderAccepted(ACLMessage msg, String conversationId) {
        OrderData order = orders.get(conversationId);

        if (order != null) {
            order.status = "PREPARANDO";
            order.orderTime = System.currentTimeMillis();

            System.out.println("✅ " + restaurantName + " - Comenzando a preparar: " + order.item);

            ACLMessage confirm = new ACLMessage(ACLMessage.CONFIRM);
            confirm.setConversationId(conversationId);
            confirm.setContent("Preparando " + order.item);
            confirm.addReceiver(gestorAgent);
            send(confirm);
        }
    }

    private void handleOrderCancellation(ACLMessage msg, String conversationId) {
        OrderData order = orders.get(conversationId);

        if (order != null) {
            order.status = "CANCELADO";
            System.out.println("❌ " + restaurantName + " - Orden cancelada: " + order.item);
            orders.remove(conversationId);
        }
    }

    private void registrarServicio() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("restaurante");
            sd.setName("venta-comida");
            dfd.addServices(sd);

            DFService.register(this, dfd);

        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void buscarGestor() {

        try {

            DFAgentDescription template =
                new DFAgentDescription();

            ServiceDescription sd =
                new ServiceDescription();

            sd.setType("gestor");

            template.addServices(sd);

            DFAgentDescription[] result =
                DFService.search(
                    this,
                    template
                );

            if (result.length > 0) {

                gestorAgent =
                    result[0].getName();

                System.out.println(
                    "✅ Gestor encontrado: "
                    + gestorAgent
                        .getLocalName()
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}