package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import java.util.HashMap;
import java.util.Map;

public class GestorAgent extends Agent {

    private Map<String, OrderData> orders = new HashMap<>();
    private Map<String, jade.core.AID> restaurants = new HashMap<>();
    private jade.core.AID deliveryAgent;

    private class OrderData {
        public String customerID;
        public String item;
        public String status;
        public jade.core.AID selectedRestaurant;
        public double price;

        OrderData(String customerID, String item) {
            this.customerID = customerID;
            this.item = item;
            this.status = "INICIAL";
            this.price = 0;
        }
    }

    protected void setup() {
        System.out.println("\n📊 " + getLocalName() + " - Gestor de Pedidos iniciado. Coordinando operaciones...");

        // Registrar servicio
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("gestor");
            sd.setName("gestion-pedidos");
            dfd.addServices(sd);

            DFService.register(this, dfd);

        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Buscar agentes disponibles
        buscarAgentes();

        // Comportamiento para gestionar mensajes
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();

                if (msg != null) {
                    String conversationId = msg.getConversationId();

                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        // Nuevo pedido del cliente
                        handleNewOrder(msg, conversationId);

                    } else if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        // Respuesta de restaurante con propuesta de precio/tiempo
                        handleRestaurantProposal(msg, conversationId);

                    } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        // Cliente aceptó la propuesta
                        handleClientAcceptance(msg, conversationId);

                    } else if (msg.getPerformative() == ACLMessage.INFORM) {
                        // Informes de estado
                        handleStatusUpdate(msg, conversationId);
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void handleNewOrder(ACLMessage msg, String conversationId) {
        OrderData order = new OrderData(msg.getSender().getLocalName(), msg.getContent());
        orders.put(conversationId, order);

        System.out.println("📍 " + getLocalName() + " - Nueva orden: " + msg.getContent() + " de "
                + msg.getSender().getLocalName());

        // Solicitar presupuestos a restaurantes
        requestQuotes(msg.getSender(), conversationId, msg.getContent());
    }

    private void requestQuotes(jade.core.AID client, String conversationId, String item) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("restaurante");
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(this, template);

            if (result.length > 0) {
                System.out.println(
                        "🔍 " + getLocalName() + " - Solicitan presupuestos a " + result.length + " restaurantes");

                for (DFAgentDescription res : result) {
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(res.getName());
                    cfp.setConversationId(conversationId);
                    cfp.setContent(item);
                    cfp.setReplyWith("cfp-" + System.currentTimeMillis());
                    send(cfp);
                }
            }

        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void handleRestaurantProposal(
            ACLMessage msg,
            String conversationId) {

        OrderData order = orders.get(conversationId);

        if (order != null) {

            String content = msg.getContent();

            System.out.println(
                "💵 " + getLocalName()
                + " - Presupuesto de "
                + msg.getSender().getLocalName()
                + ": " + content
            );

            double price = extractPrice(content);

            if (order.selectedRestaurant == null
                || price < order.price) {

                order.selectedRestaurant =
                    msg.getSender();

                order.price = price;

                System.out.println(
                    "🏆 " + getLocalName()
                    + " seleccionó "
                    + msg.getSender()
                        .getLocalName()
                    + " con precio "
                    + price
                );
            }

            if (order.status.equals("INICIAL")) {

                order.status =
                    "RESTAURANTE_SELECCIONADO";

                ACLMessage propose =
                    new ACLMessage(
                        ACLMessage.PROPOSE
                    );

                propose.addReceiver(
                    new jade.core.AID(
                        order.customerID,
                        false
                    )
                );

                propose.setConversationId(
                    conversationId
                );

                propose.setContent(content);

                send(propose);

                System.out.println(
                    "💬 Propuesta enviada "
                    + "al cliente"
                );
            }
        }
    }

    private void handleClientAcceptance(ACLMessage msg, String conversationId) {
        OrderData order = orders.get(conversationId);

        if (order != null) {
            order.status = "ACEPTADO_POR_CLIENTE";

            // Confirmar con restaurante
            ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            confirm.addReceiver(order.selectedRestaurant);
            confirm.setConversationId(conversationId);
            confirm.setContent(order.item);
            send(confirm);

            System.out.println(
                    "✔️ " + getLocalName() + " - Confirmó orden con " + order.selectedRestaurant.getLocalName());
        }
    }

    private void handleStatusUpdate(ACLMessage msg, String conversationId) {
        OrderData order = orders.get(conversationId);

        if (order != null) {
            String content = msg.getContent();

            if (content.contains("listo")) {
                order.status = "LISTO_PARA_ENTREGA";

                // Notificar repartidor
                if (deliveryAgent != null) {
                    ACLMessage delivery = new ACLMessage(ACLMessage.REQUEST);
                    delivery.addReceiver(deliveryAgent);
                    delivery.setConversationId(conversationId);
                    delivery.setContent(order.customerID + "|" + order.item);
                    send(delivery);

                    System.out.println("🚚 " + getLocalName() + " - Enviado a repartidor para entrega");
                }
            }
        }
    }

    private void buscarAgentes() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("repartidor");
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                deliveryAgent = result[0].getName();
                System.out.println("✅ Repartidor encontrado: " + deliveryAgent.getLocalName());
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private double extractPrice(String content) {
        try {
            String pricePart =
                content.split("\\|")[0];

            pricePart =
                pricePart.replace(
                    "Precio: $",
                    ""
                );

            return Double.parseDouble(
                pricePart.trim()
            );

        } catch (Exception e) {

            return 9999;

        }
    }
}