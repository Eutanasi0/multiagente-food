package agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RepartidorAgent extends Agent {

    private Map<String, DeliveryData> deliveries = new HashMap<>();
    private int avgDeliveryTime = 15; // minutos
    private jade.core.AID gestorAgent;

    private class DeliveryData {
        public String customerID;
        public String item;
        public String status;
        public long startTime;
        public int estimatedTime;

        DeliveryData(String customerID, String item, int estimatedTime) {
            this.customerID = customerID;
            this.item = item;
            this.status = "RECOGIDO";
            this.startTime = System.currentTimeMillis();
            this.estimatedTime = estimatedTime;
        }
    }

    protected void setup() {
        System.out.println("\n🚴 " + getLocalName() + " - Repartidor en servicio. Listo para entregas!");

        registrarServicio();
        buscarGestor();

        // Comportamiento para recibir órdenes de entrega
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();

                if (msg != null) {
                    String conversationId = msg.getConversationId();

                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        handleDeliveryRequest(msg, conversationId);

                    } else if (msg.getPerformative() == ACLMessage.CANCEL) {
                        handleDeliveryCancellation(msg, conversationId);
                    }
                } else {
                    block();
                }
            }
        });

        // Comportamiento para procesar entregas en progreso
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                java.util.Iterator<String> it = deliveries.keySet().iterator();

                while (it.hasNext()) {
                    String deliveryId = it.next();
                    DeliveryData delivery = deliveries.get(deliveryId);

                    if (delivery.status.equals("EN_CAMINO")) {
                        long elapsedTime = (System.currentTimeMillis() - delivery.startTime) / 1000 / 60; // minutos

                        // Enviar actualizaciones de progreso
                        if (elapsedTime % 5 == 0 && elapsedTime > 0) {
                            int remaining = delivery.estimatedTime - (int) elapsedTime;
                            if (remaining > 0) {
                                ACLMessage update = new ACLMessage(ACLMessage.AGREE);
                                update.setConversationId(deliveryId);
                                update.setContent(
                                        "En camino hacia " + delivery.customerID + ". ETA: " + remaining + " minutos");
                                update.addReceiver(gestorAgent);
                                send(update);

                                System.out.println("📍 " + getLocalName() + " - En ruta para " + delivery.customerID
                                        + " (ETA: " + remaining + " min)");
                            }
                        }

                        // Completar entrega
                        if (elapsedTime >= delivery.estimatedTime) {

                            delivery.status = "ENTREGADO";

                            ACLMessage delivered = new ACLMessage(ACLMessage.INFORM);
                            delivered.setConversationId(deliveryId);
                            delivered.setContent(
                                    "Entregado a "
                                            + delivery.customerID
                                            + ": "
                                            + delivery.item);

                            delivered.addReceiver(gestorAgent);

                            delivered.addReceiver(
                                    new jade.core.AID(
                                            delivery.customerID,
                                            false));

                            send(delivered);

                            System.out.println(
                                    "✅ "
                                            + getLocalName()
                                            + " - Entregado a "
                                            + delivery.customerID
                                            + ": "
                                            + delivery.item);

                            it.remove();
                        }
                    }
                }

                block(5000); // Revisa cada 5 segundos
            }
        });
    }

    private void handleDeliveryRequest(ACLMessage msg, String conversationId) {
        String content = msg.getContent();
        String[] parts = content.split("\\|");

        if (parts.length == 2) {
            String customerID = parts[0].trim();
            String item = parts[1].trim();

            Random r = new Random();
            int deliveryTime = avgDeliveryTime + r.nextInt(10);

            DeliveryData delivery = new DeliveryData(customerID, item, deliveryTime);
            delivery.status = "EN_CAMINO";
            deliveries.put(conversationId, delivery);

            System.out.println("🚚 " + getLocalName() + " - Recogé " + item + " para " + customerID + ". ETA: "
                    + deliveryTime + " minutos");

            // Confirmar recepción de orden
            ACLMessage confirm = msg.createReply();
            confirm.setPerformative(ACLMessage.CONFIRM);
            confirm.setContent("En ruta hacia " + customerID);
            send(confirm);

            // Notificar al cliente que está en camino
            ACLMessage notify = new ACLMessage(ACLMessage.AGREE);
            notify.setConversationId(conversationId);
            notify.addReceiver(new jade.core.AID(customerID, false));
            notify.setContent("Tu pedido " + item + " está en camino! Llegará en ~" + deliveryTime + " minutos");
            send(notify);
        }
    }

    private void handleDeliveryCancellation(ACLMessage msg, String conversationId) {
        DeliveryData delivery = deliveries.get(conversationId);

        if (delivery != null) {
            delivery.status = "CANCELADO";
            System.out.println("❌ " + getLocalName() + " - Entrega cancelada para " + delivery.customerID);
            deliveries.remove(conversationId);
        }
    }

    private void buscarGestor() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("gestor");
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                gestorAgent = result[0].getName();
                System.out.println("✅ Gestor encontrado: " + gestorAgent.getLocalName());
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void registrarServicio() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("repartidor");
            sd.setName("delivery");
            dfd.addServices(sd);

            DFService.register(this, dfd);

        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}