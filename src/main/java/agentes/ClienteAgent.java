package agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import java.util.HashMap;
import java.util.Map;

public class ClienteAgent extends Agent {

    private String orderID;
    private Map<String, String> orderStatus = new HashMap<>();
    private String[] menuItems = { "Hamburguesa", "Pizza", "Ensalada", "Sushi", "Tacos" };

    protected void setup() {
        System.out.println("\n🛵 " + getLocalName() + " - Cliente iniciado. Hambriento y listo para ordenar!");

        // Generar ID único para la orden
        orderID = getLocalName() + "_" + System.currentTimeMillis();

        // Comportamiento para hacer pedido
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("gestor");
                    template.addServices(sd);

                    DFAgentDescription[] result = DFService.search(myAgent, template);

                    if (result.length > 0) {
                        String selectedItem = menuItems[(int) (Math.random() * menuItems.length)];

                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.addReceiver(result[0].getName());
                        msg.setConversationId(orderID);
                        msg.setContent(selectedItem);
                        msg.setReplyWith("pedido-" + System.currentTimeMillis());

                        send(msg);

                        System.out.println(
                                "📱 " + getLocalName() + " solicitó: " + selectedItem + " (Order ID: " + orderID + ")");
                        orderStatus.put(orderID, "PEDIDO_ENVIADO");

                    } else {
                        System.out.println("❌ " + getLocalName() + " - No se encontró gestor");
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        });

        // Comportamiento para escuchar respuestas
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();

                if (msg != null) {
                    String conversationId = msg.getConversationId();

                    if (msg.getPerformative() == ACLMessage.INFORM) {
                        System.out.println("✅ " + getLocalName() + " recibió: " + msg.getContent());
                        orderStatus.put(conversationId, "ORDEN_CONFIRMADA");

                    } else if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        System.out.println("💰 " + getLocalName() + " - Propuesta recibida: " + msg.getContent());
                        orderStatus.put(conversationId, "PRESUPUESTO_RECIBIDO");

                        // Cliente acepta propuesta
                        ACLMessage accept = msg.createReply();
                        accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        accept.setContent("Aceptado. Proceder con la orden.");
                        send(accept);
                        System.out.println("👍 " + getLocalName() + " - Aceptó la propuesta");

                    } else if (msg.getPerformative() == ACLMessage.CONFIRM) {
                        System.out.println("🍴 " + getLocalName() + " - Confirmación final: " + msg.getContent());
                        orderStatus.put(conversationId, "PREPARANDO");

                    } else if (msg.getPerformative() == ACLMessage.AGREE) {
                        System.out.println("🚚 " + getLocalName() + " - Entrega en progreso: " + msg.getContent());
                        orderStatus.put(conversationId, "EN_ENTREGA");

                    } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Entregado")) {
                        System.out.println("🎉 " + getLocalName() + " - Pedido recibido: " + msg.getContent());
                        orderStatus.put(conversationId, "ENTREGADO");
                    }
                } else {
                    block();
                }
            }
        });
    }
}
